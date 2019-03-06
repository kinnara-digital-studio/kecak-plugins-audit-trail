<link rel="stylesheet" type="text/css" href="${request.contextPath}/plugin/${className}/css/auditTrailProgress.css">
<div class="form-cell timeline block-content-full" ${elementMetaData}>
	<ul id="${elementParamName}" name="${elementParamName}" class="${elementParamName}${element.properties.elementUniqueKey!} timeline-list">
	<#list auditList as auditRow >
		<li class="timeline-content">
			<div class="text-light-op">
				<img src="data:image/jpeg;base64,${auditRow.image}" onerror="this.src='${request.contextPath}/plugin/${className}/images/default-avatar.png'" class="timeline-icon">
			</div>
			<span class="push-bit">${auditRow.status}</span><br>
			<span class="timeline-time date">${auditRow.date}</span><br>
			<span class="timeline-time time">${auditRow.time}</span>
		</li>
	</#list>
	</ul>
</div>