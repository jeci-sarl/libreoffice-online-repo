package fr.arawa.lool.alfresco;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import dk.magenta.libreoffice.online.LOOLPutFileWebScript;

/**
 * Remove automatic and explicit versions.
 * 
 * @author Jeremie Lesage
 *
 */
public class CleanVersionWebScript extends DeclarativeWebScript {
    private static final Log logger = LogFactory.getLog(CleanVersionWebScript.class);

    private static final String PARAM_STORE_TYPE = "store_type";
    private static final String PARAM_STORE_ID = "store_id";
    private static final String PARAM_ID = "id";
    private static final String PARAM_KEEP_EXP = "keep_exp";
    private static final String PARAM_KEEP_AUTO = "keep_auto";

    private ServiceRegistry serviceRegistry;

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        final Map<String, Object> model = new HashMap<>();

        final Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
        final String storeType = WebscriptHelper.getParam(templateArgs, PARAM_STORE_TYPE);
        final String storeId = WebscriptHelper.getParam(templateArgs, PARAM_STORE_ID);
        final String guid = WebscriptHelper.getParam(templateArgs, PARAM_ID);
        final NodeRef nodeRef = new NodeRef(storeType, storeId, guid);

        // Number automatique version to keep
        Integer keepAuto = WebscriptHelper.intergerValue(templateArgs, PARAM_KEEP_AUTO);
        keepAuto = keepAuto == null ? -1 : keepAuto;

        // Number explicit version to keep
        Integer keepExp = WebscriptHelper.intergerValue(templateArgs, PARAM_KEEP_EXP);
        keepExp = keepExp == null ? -1 : keepExp;

        // Removing version by using Alfresco Java API
        final VersionService versionService = serviceRegistry.getVersionService();
        final VersionHistory history = versionService.getVersionHistory(nodeRef);

        int countAuto = 0;
        int countExp = 0;
        for (Version version : history.getAllVersions()) {
            Serializable loolautosave = version.getVersionProperties().get(LOOLPutFileWebScript.LOOL_AUTOSAVE);
            if (loolautosave == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("v." + version.getVersionLabel() + " - keep");
                }

                // Not Lool Version, ignoring
                continue;
            }

            Boolean autosave = (Boolean) loolautosave;
            if (autosave && keepAuto >= 0) {
                // Removing old auto-save version
                if (++countAuto > keepAuto) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("v." + version.getVersionLabel() + " - remove auto");
                    }

                    versionService.deleteVersion(nodeRef, version);
                }
            }

            if (!autosave && keepExp >= 0) {
                // Removing old save version (only from collabora)
                if (++countExp > keepExp) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("v." + version.getVersionLabel() + " - remove explicit");
                    }

                    versionService.deleteVersion(nodeRef, version);
                }
            }
        }

        model.put("success", "true");
        return model;

    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

}
