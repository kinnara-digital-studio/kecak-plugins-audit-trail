<div class="form-cell" ${elementMetaData!}>

<#if error=='false'>
<link rel="stylesheet" type="text/css" href="${request.contextPath}/plugin/com.kecak.enterprise.ApproverAuditrail/css/jquery.dataTables.min.css">
<script type="text/javascript" src="${request.contextPath}/plugin/com.kecak.enterprise.ApproverAuditrail/js/jquery.dataTables.min.js"></script>
<script type="text/javascript">
$(document).ready(function(){
    $('#${elementParamName!}').DataTable({
        "pagingType": "full_numbers",
        "order": [[ ${sort!}, "desc" ]]
    });
});

</script>
<table id="${elementParamName!}" class="table table-striped table-bordered" cellspacing="0" width="100%">
    <thead>
        <tr>
            <#list headers as header>
            <th>${header}</th>
            </#list>
        </tr>
    </thead>
    <tbody>
         <#list datas as data>
        <tr>
           
                <#list data as content>
                    <td>${content}</td>
                </#list>
        </tr>
        </#list>
    </tbody>
</table>
<#else>
<table id="${elementParamName!}">
    <thead>
        <tr>
            <#list headers as header>
            <th>${header}</th>
            </#list>
        </tr>
    </thead>
</table>
</#if>
</div>
