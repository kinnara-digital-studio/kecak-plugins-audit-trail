<#if includeMetaData!>
<div class="form-cell" ${elementMetaData!}>
    <label class="label">${element.properties.label} Auditrail Ace Timeline</label>
</div>
<#else>
<div class="form-cell widget-box" ${elementMetaData!}>
	<div class="widget-header">
		<h4 class="lighter smaller">
			<i class="fa-comments blue"></i>
			${element.properties.label}
		</h4>
	</div>
	<div class="widget-body">
		<div class="widget-main no-padding">
			<div class="dialogs">
				<#list auditrailData! as data>
					<div class="itemdiv dialogdiv">
						<div class="user">
							<img alt="Alexa's Avatar" src="${request.contextPath}/plugin/${className}/images/default-avatar.png" />
						</div>
	
						<div class="body">
							<div class="time">
								<i class="icon-time"></i>
								<span class="green">4 sec</span>
							</div>
	
							<div class="name">
								<a href="#">Alexa</a>
							</div>
							<div class="text">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque commodo massa sed ipsum porttitor facilisis.</div>
	
							<div class="tools">
								<a href="#" class="btn btn-minier btn-info">
									<i class="icon-only icon-share-alt"></i>
								</a>
							</div>
						</div>
					</div>
				</#list>
			</div>
		</div><!--/widget-main-->
	</div><!--/widget-body-->
</div>
</#if>