package dk.magenta.libreoffice.online.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Contains just two methods for now but more maybe added later as we may want
 * to perform several actions from the token context as opposed from the
 * LOOLService context
 *
 * @author DarkStar1.
 */
public interface WOPITokenService {
    String ACCESS_TOKEN = "access_token";
    String FILE_ID = "fileId";

    /**
     * Will return a file nodeRef for the Token in question
     * 
     * @param tokenInfo
     * @return
     * @deprecated {@link LOOLService#getFileNodeRef(WOPIAccessTokenInfo)}
     */
    @Deprecated
    NodeRef getFileNodeRef(WOPIAccessTokenInfo tokenInfo);

    /**
     * Returns a PersonInfo for the token in question
     * 
     * @param tokenInfo
     * @return
     * @deprecated {@link LOOLService#getUserInfoOfToken(WOPIAccessTokenInfo)}
     */
    @Deprecated
    PersonInfo getUserInfoOfToken(WOPIAccessTokenInfo tokenInfo);

    /**
     * Gets a token from the request params
     * 
     * @param req
     * @return
     * @deprecated We don't want Token not checked
     *             {@link LOOLService#checkAccessToken(WebScriptRequest)}
     */
    @Deprecated
    WOPIAccessTokenInfo getTokenInfo(WebScriptRequest req);

}
