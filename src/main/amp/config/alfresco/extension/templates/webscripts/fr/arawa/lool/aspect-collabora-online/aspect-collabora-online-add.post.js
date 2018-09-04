<import resource="classpath:/alfresco/extension/templates/webscripts/fr/arawa/lool/aspect-collabora-online/aspect-collabora-online.lib.js">

function editNode(result, node) {
    result.action = "addAspect";

    if (!node.hasAspect(ASPECT_LOOL)) {
        node.addAspect(ASPECT_LOOL);
    }
}

main();
