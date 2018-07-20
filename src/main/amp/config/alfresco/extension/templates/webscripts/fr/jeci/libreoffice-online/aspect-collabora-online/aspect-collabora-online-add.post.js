<import resource="classpath:/alfresco/extension/templates/webscripts/fr/jeci/libreoffice-online/aspect-collabora-online/aspect-collabora-online.lib.js">

function editNode(node) {
    if (!node.hasAspect(ASPECT_LOOL)) {
        node.addAspect(ASPECT_LOOL);
    }
}

main();