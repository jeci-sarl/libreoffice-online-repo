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

    private static final String LOOL_AUTOSAVE = "lool:autosave";

    @Override
    public void execute(final WebScriptRequest req, WebScriptResponse res) throws IOException {

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
                // https://community.alfresco.com/message/809749-re-why-is-the-modifier-of-a-content-a-random-user-from-the-list-of-logged-in-users?commentID=809749&et=watches.email.thread#comment-809749
                retryingTransactionHelper
                        .doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
                            @Override
                            public Void execute() throws Throwable {
                                try {
                                    AuthenticationUtil.setFullyAuthenticatedUser(tokenInfo.getUserName());
                                    ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT,
                                            true);
                                    writer.putContent(req.getContent().getInputStream());
                                    writer.guessMimetype(
                                            (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
                                    writer.guessEncoding();

                                    Map<String, Serializable> versionProperties = new HashMap<String, Serializable>(2);
                                    versionProperties.put(VersionModel.PROP_VERSION_TYPE, VersionType.MINOR);
                                    if (isAutosave) {
                                        versionProperties.put(VersionModel.PROP_DESCRIPTION, "Edit with Collabora");
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