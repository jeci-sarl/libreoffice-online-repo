package dk.magenta.libreoffice.online.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author DarkStar1.
 * @deprecated Merge into
 *             {@link LOOLService#getFileNodeRef(WOPIAccessTokenInfo)}
 */
@Deprecated
public class WOPITokenServiceImpl implements WOPITokenService {
    private LOOLService loolService;

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
     * @deprecated move to
     *             {@link LOOLServiceImpl#getUserInfoOfToken(WOPIAccessTokenInfo)}
     */
    @Deprecated
    @Override
    public PersonInfo getUserInfoOfToken(WOPIAccessTokenInfo tokenInfo) {
        return loolService.getUserInfoOfToken(tokenInfo);
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
        final String fileId = req.getServiceMatch().getTemplateVars().get(LOOLService.FILE_ID);
        final String accessToken = req.getParameter(LOOLService.ACCESS_TOKEN);

        return loolService.getAccessToken(accessToken, fileId);
    }
}
