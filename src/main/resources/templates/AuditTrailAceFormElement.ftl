<#if includeMetaData!>
<div class="form-cell" ${elementMetaData!}>
    <label class="label">${element.properties.label} Auditrail Ace Timeline</label>
</div>
<#else>
<div class="timeline-container  ace-scroll ${elementMetaData!}">
	<div class="timeline-label">
		<span class="label label-primary arrowed-in-right label-lg">
			<b>${element.properties.label}</b>
		</span>
	</div>

	<div class="timeline-items">
	<div class="scroll-content" style="max-height: ${element.properties.height};">
	<#list datas! as data>
		<div class="timeline-item clearfix">
			<div class="timeline-info">
				<img alt="User Avatar" src="${request.contextPath}/plugin/${className}/images/default-avatar.png" />
			</div>

			<div class="widget-box transparent">
				<div class="widget-header widget-header-small">
					<h5 class="widget-title smaller">
						<a href="#" class="blue">${data.performer!}</a>
						<span class="grey">${data.status!}</span>
					</h5>

					<span class="widget-toolbar no-border">
						<i class="ace-icon fa fa-clock-o bigger-110"></i>
						16:22
					</span>

					<span class="widget-toolbar">
						<a href="#" data-action="reload">
							<i class="ace-icon fa fa-refresh"></i>
						</a>

						<a href="#" data-action="collapse">
							<i class="ace-icon fa fa-chevron-up"></i>
						</a>
					</span>
				</div>

				<div class="widget-body">
					<div class="widget-main">
						Anim pariatur cliche reprehenderit, enim eiusmod
						<span class="red">high life</span>

						accusamus terry richardson ad squid &hellip;
						<!--<div class="space-6"></div>

						<div class="widget-toolbox clearfix">
							<div class="pull-left">
								<i class="ace-icon fa fa-hand-o-right grey bigger-125"></i>
								<a href="#" class="bigger-110">Click to read &hellip;</a>
							</div>

							<div class="pull-right action-buttons">
								<a href="#">
									<i class="ace-icon fa fa-check green bigger-130"></i>
								</a>

								<a href="#">
									<i class="ace-icon fa fa-pencil blue bigger-125"></i>
								</a>

								<a href="#">
									<i class="ace-icon fa fa-times red bigger-125"></i>
								</a>
							</div>
						</div>-->
					</div>
				</div>
			</div>
		</div>
	</#list>
	</div>
	</div><!-- /.timeline-items -->
</div><!-- /.timeline-container -->
</#if>