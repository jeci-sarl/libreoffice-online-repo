package dk.magenta.libreoffice.online;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.DeclarativeWebScript;

import dk.magenta.libreoffice.online.service.WOPIAccessTokenInfo;

public abstract class LOOLAbstractWebScript extends DeclarativeWebScript {
    protected NodeService nodeService;

    protected Map<QName, Serializable> runAsGetProperties(final WOPIAccessTokenInfo wopiToken, final NodeRef nodeRef) {
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Map<QName, Serializable>>() {
            @Override
            public Map<QName, Serializable> doWork() throws Exception {
                return nodeService.getProperties(nodeRef);
            }

        }, wopiToken.getUserName());
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
}
