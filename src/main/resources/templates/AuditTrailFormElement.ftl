<#assign elementId = elementParamName + "_" + element.properties.elementUniqueKey >

<#if includeMetaData>
    <div class="form-cell" ${elementMetaData!}>
        <table id="" class="table table-striped table-bordered" cellspacing="0" width="100%">
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
<#elseif !hidden >
    <div class="form-cell" ${elementMetaData!}>
        <label class="label" style="margin-bottom: 13px;">${element.properties.label!?html}</label>

        <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.10.22/css/jquery.dataTables.min.css"/>
        <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/buttons/1.6.5/css/buttons.dataTables.min.css"/>

        <script type="text/javascript" src="https://cdn.datatables.net/1.10.22/js/jquery.dataTables.min.js"></script>
        <script type="text/javascript" src="https://cdn.datatables.net/buttons/1.6.5/js/dataTables.buttons.min.js"></script>

        <script type="text/javascript">
            $(document).ready(function(){
                try {
                    $('table#${elementId}').DataTable({
                        "pagingType": "full_numbers",
                        "ordering" : false
                    });
                } catch (ignored) {}

                <#-- $('table#${elementId}').DataTable({"pagingType": "full_numbers"});  -->
            });

        </script>

        <table id="${elementId}" class="table table-striped table-bordered" cellspacing="0" width="100%">
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
