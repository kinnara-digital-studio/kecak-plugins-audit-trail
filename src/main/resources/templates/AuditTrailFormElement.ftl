<#if !hidden >
    <div class="form-cell" ${elementMetaData!}>
        <label class="label" style="margin-bottom: 13px;">${element.properties.label!?html}</label>

        <link rel="stylesheet" type="text/css" href="${request.contextPath}/plugin/${className}/css/jquery.dataTables.min.css">
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/jquery.dataTables.min.js"></script>
        <script type="text/javascript">
        $(document).ready(function(){
            try {
                $('table#${elementParamName!}${element.properties.elementUniqueKey!}').DataTable({
                    "pagingType": "full_numbers",
                    "ordering" : false
                });
            } catch (err) {
                // do nothing
            }

            <#-- $('table#${elementParamName!}${element.properties.elementUniqueKey!}').DataTable({"pagingType": "full_numbers"});  -->
        });

        </script>
        <table id="${elementParamName!}${element.properties.elementUniqueKey!}" class="table table-striped table-bordered" cellspacing="0" width="100%">
            <thead>
                <tr>
                    <#list headers as header>
                    <th>${header!}</th>
                    </#list>
                </tr>
            </thead>
            <tbody>
                 <#list datas as data>
                <tr>
                        <#list data! as content>
                            <td>${content!}</td>
                        </#list>
                </tr>
                </#list>
            </tbody>
        </table>
    </div>
</#if>
