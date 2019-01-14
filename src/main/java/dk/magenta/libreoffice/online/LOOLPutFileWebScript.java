/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package dk.magenta.libreoffice.online;

import java.io.IOException;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import dk.magenta.libreoffice.online.service.PersonInfo;
import dk.magenta.libreoffice.online.service.WOPIAccessTokenInfo;
import dk.magenta.libreoffice.online.service.WOPITokenService;

public class LOOLPutFileWebScript extends AbstractWebScript implements WOPIConstant {

    private static final Log logger = LogFactory.getLog(LOOLPutFileWebScript.class);

    private WOPITokenService wopiTokenService;
    private NodeService nodeService;
    private ContentService contentService;
    private VersionService versionService;
    private RetryingTransactionHelper retryingTransactionHelper;

    public static final String LOOL_AUTOSAVE = "lool:autosave";
    public static final String AUTOSAVE_DESCRIPTION = "Edit with Collabora";

    private DateTimeFormatter iso8601formater = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC);

    @Override
    public void execute(final WebScriptRequest req, final WebScriptResponse res) throws IOException {

        final String wopiOverrideHeader = req.getHeader(X_WOPI_OVERRIDE);

        if (wopiOverrideHeader == null || !wopiOverrideHeader.equals("PUT")) {
            throw new WebScriptException(X_WOPI_OVERRIDE + " header must be present and equal to 'PUT'");
        }

        /*
         * will have the value 'true' when the PutFile is triggered by autosave, and
         * 'false' when triggered by explicit user operation (Save button or menu
         * entry).
         */
        final String hdrAutosave = req.getHeader(X_LOOL_WOPI_IS_AUTOSAVE);
        final boolean isAutosave = hdrAutosave != null && Boolean.parseBoolean(hdrAutosave.trim());

        if (logger.isDebugEnabled()) {
            logger.debug("Request " + (isAutosave ? "is" : "is not") + " AUTOSAVE");
        }

        try {
            final WOPIAccessTokenInfo tokenInfo = wopiTokenService.getTokenInfo(req);
            // Verifying that the user actually exists
            final PersonInfo person = wopiTokenService.getUserInfoOfToken(tokenInfo);
            final NodeRef nodeRef = wopiTokenService.getFileNodeRef(tokenInfo);
            if (StringUtils.isBlank(person.getUserName()) && person.getUserName() != tokenInfo.getUserName()) {
                throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
                        "The user no longer appears to exist.");
            }

            if (tokenInfo != null) {

                boolean success = checkTimestamp(req, res, nodeRef);

                if (success) {
                    writeFileToDisk(req, isAutosave, tokenInfo, nodeRef);
                    responseNewModifiedTime(res, nodeRef);
                }

            }

            if (logger.isInfoEnabled()) {
                logger.info("Modifier for the above nodeRef [" + nodeRef.toString() + "] is: "
                        + nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIER));
            }

        } catch (ContentIOException we) {
            final String msg = "Error writing to file";
            logger.error(msg, we);
            throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, msg);
        } catch (WebScriptException we) {
            final String msg = "Access token invalid or expired";
            logger.error(msg, we);
            throw new WebScriptException(Status.STATUS_UNAUTHORIZED, msg);
        } catch (NullPointerException we) {
            final String msg = "Unidentified problem writing to file please consult system administrator for help on this issue";
            logger.error(msg, we);
            throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, msg);
        }
    }

    /**
     * Write content file to disk on set version properties.
     * https://community.alfresco.com/message/809749-re-why-is-the-modifier-of-a-content-a-random-user-from-the-list-of-logged-in-users?commentID=809749&et=watches.email.thread#comment-809749
     * 
     * @param req
     * @param isAutosave
     *            id true, set PROP_DESCRIPTION, "Edit with Collabora"
     * @param tokenInfo
     * @param nodeRef
     */
    private void writeFileToDisk(final WebScriptRequest req, final boolean isAutosave,
            final WOPIAccessTokenInfo tokenInfo, final NodeRef nodeRef) {

        retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
            @Override
            public Void execute() throws Throwable {
                try {
                    AuthenticationUtil.setFullyAuthenticatedUser(tokenInfo.getUserName());
                    ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
                    writer.putContent(req.getContent().getInputStream());
                    writer.guessMimetype((String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
                    writer.guessEncoding();

                    Map<String, Serializable> versionProperties = new HashMap<String, Serializable>(2);
                    versionProperties.put(VersionModel.PROP_VERSION_TYPE, VersionType.MINOR);
                    if (isAutosave) {
                        versionProperties.put(VersionModel.PROP_DESCRIPTION, AUTOSAVE_DESCRIPTION);
                    }
                    versionProperties.put(LOOL_AUTOSAVE, isAutosave);
                    versionService.createVersion(nodeRef, versionProperties);

                } finally {
                    AuthenticationUtil.clearCurrentSecurityContext();
                }
                return null;
            }
        }, false, true);
    }

    /**
     * Return New PROP_MODIFIED to lool
     * 
     * @param res
     * @param nodeRef
     */
    private void responseNewModifiedTime(final WebScriptResponse res, final NodeRef nodeRef) {
        retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
            @Override
            public Void execute() throws Throwable {

                Date newModified = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED);
                final String dte = iso8601formater.format(Instant.ofEpochMilli(newModified.getTime()));

                jsonResponse(res, 200, "{ \"LastModifiedTime\": \"" + dte + "\" }");

                return null;
            }
        }, true, true);
    }

    /**
     * Check if X-LOOL-WOPI-Timestamp is equal to PROP_MODIFIED
     * 
     * @param req
     * @param res
     * @param nodeRef
     * @return false if error, and write response output with code 409
     * @throws IOException
     */
    private boolean checkTimestamp(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef)
            throws IOException {

        final String hdrTimestamp = req.getHeader(X_LOOL_WOPI_TIMESTAMP);
        if (hdrTimestamp == null) {
            // Ignore if no X-LOOL-WOPI-Timestamp
            return true;
        }
        LocalDate loolTimestamp = null;
        try {
            loolTimestamp = LocalDate.from(iso8601formater.parse(hdrTimestamp));
        } catch (DateTimeException e) {
            logger.error("checkTimestamp Error : " + e.getMessage());
        }

        if (loolTimestamp == null) {
            jsonResponse(res, 409, "{ \"LOOLStatusCode\": 1010 }");
            return false;
        }

        // Check X_LOOL_WOPI_TIMESTAMP header
        final Date modified = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED);
        final LocalDate localDate = ((Date) modified).toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        if (loolTimestamp.compareTo(localDate) != 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("PROP_MODIFIED : " + modified);
                logger.debug(X_LOOL_WOPI_TIMESTAMP + " : " + hdrTimestamp);
            }
            logger.error("checkTimestamp Error : " + X_LOOL_WOPI_TIMESTAMP + " is different than PROP_MODIFIED");
            jsonResponse(res, 409, "{ \"LOOLStatusCode\": 1010 }");
            return false;
        }

        return true;
    }

    private void jsonResponse(final WebScriptResponse res, int code, String response) throws IOException {
        res.reset();
        res.setStatus(code);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().append(response);
    }

    public void setWopiTokenService(WOPITokenService wopiTokenService) {
        this.wopiTokenService = wopiTokenService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setVersionService(VersionService versionService) {
        this.versionService = versionService;
    }

    public void setRetryingTransactionHelper(RetryingTransactionHelper retryingTransactionHelper) {
        this.retryingTransactionHelper = retryingTransactionHelper;
    }
}