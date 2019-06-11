<import resource="classpath:/alfresco/extension/templates/webscripts/fr/arawa/lool/aspect-collabora-online/aspect-collabora-online.lib.js">

function editNode(result, node) {
    result.action = "remAspect";

    if (node.hasAspect(ASPECT_LOOL)) {
        node.removeAspect(ASPECT_LOOL);
    }
}

main();
