<div class="form-cell" ${elementMetaData!}>
    <#if includeMetaData>
        <span class="form-floating-label">${elementLabel!}</span>
    <#else>
        <div class="content" style="visibility:${(isHidden == 'true')?then('hidden','visible')}">
            <span class="form-floating-label">${elementLabel!}</span>
            <#assign elementId = elementParamName + "_" + element.properties.elementUniqueKey>
            <label class="label">${element.properties.label}</label>
            ${message!}

            <#if isShowingHistoryList!false >
                <a id='${elementId}_popover_link' href="#" profile-content="${elementId}_popup_content" data-toggle="popover" >Show History...</a>
                <link rel="stylesheet" type="text/css" href="${request.contextPath}/plugin/${className}/css/valueHistoryElement.css" />
                <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/bootstrap.min.js"></script>
                <div id="${elementId}_popup_content" class="popup-content" style="display:none">
                    <table>
                        <tbody>
                            <#list historyList! as historyItem>
                                <tr>
                                    <#list historyItem?values as value>
                                        <td>${value?string}</td>
                                    </#list>
                                </tr>
                            </#list>
                        </tbody>
                    </table>
                </div>

                <script type="text/javascript">
                    $('a#${elementId}_popover_link').on('click', function(e) {e.preventDefault(); return true;});
                    $(document).ready(function(){
                        $('#${elementId}_popover_link[data-toggle="popover"]').popover({
                            trigger : 'focus',
                            html : true,
                            content : () => $('#${elementId}_popup_content.popup-content').html()
                        });
                    });
                </script>
            </#if>
        </div>
    </#if>
</div>

