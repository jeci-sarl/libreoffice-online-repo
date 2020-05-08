package dk.magenta.libreoffice.online.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author DarkStar1.
 */
public class WOPITokenServiceImpl implements WOPITokenService {
    private static final Log logger = LogFactory.getLog(WOPITokenServiceImpl.class);

    private NodeService nodeService;
    private PersonService personService;
    private LOOLService loolService;

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setLoolService(LOOLService loolService) {
        this.loolService = loolService;
    }

    /**
     * Will return a file nodeRef for the Token in question
     *
     * @param tokenInfo
     * @return
     * @deprecated move to
     *             {@link LOOLServiceImpl#getFileNodeRef(WOPIAccessTokenInfo)}
     */
    @Deprecated
    @Override
    public NodeRef getFileNodeRef(WOPIAccessTokenInfo tokenInfo) {
        return loolService.getFileNodeRef(tokenInfo);
    }

    /**
     * Returns a PersonInfo for the token in question
     *
     * @param tokenInfo
     * @return
     */
    @Override
    public PersonInfo getUserInfoOfToken(WOPIAccessTokenInfo tokenInfo) {
        final String userName = tokenInfo.getUserName();
        try {
            final NodeRef personNode = personService.getPerson(userName);
            return new PersonInfo(personService.getPerson(personNode));
        } catch (NoSuchPersonException npe) {
            logger.error("Unable to retrieve person from user id [" + userName + "] specified in token.", npe);
            throw new NoSuchPersonException(
                    "Unable to verify that the person exists. Please contact the system administrator");
        }
    }

    /**
     * Gets a token from the request params
     *
     * @param req
     * @return
     * @deprecated We don't want Token not checked
     *             {@link LOOLServiceImpl#checkAccessToken(WebScriptRequest)}
     */
    @Override
    @Deprecated
    public WOPIAccessTokenInfo getTokenInfo(WebScriptRequest req) {
        final String fileId = req.getServiceMatch().getTemplateVars().get(FILE_ID);
        final String accessToken = req.getParameter(ACCESS_TOKEN);

        return loolService.getAccessToken(accessToken, fileId);
    }
}
