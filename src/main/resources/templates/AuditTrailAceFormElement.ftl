<#if includeMetaData!>
<div class="form-cell" ${elementMetaData!}>
    <label class="label">${element.properties.label} Auditrail Ace Timeline</label>
</div>
<#else>
<div class="timeline-container ${elementMetaData!}">
	<div class="timeline-label">
		<span class="label label-primary arrowed-in-right label-lg">
			<b>${element.properties.label}</b>
		</span>
	</div>

	<div class="timeline-items">
	<#list datas! as data>
		<div class="timeline-item clearfix">
			<div class="timeline-info">
				<img alt="User Avatar" src="${data.avatar!}" />
			</div>

			<div class="widget-box transparent">
				<div class="widget-header widget-header-small">
					<h5 class="widget-title smaller">
						<a href="#" class="blue">${data.processName!} - ${data.performer!}</a>
						<span class="grey"></span>
					</h5>

					<span class="widget-toolbar no-border">
						<i class="ace-icon fa fa-clock-o bigger-110"></i>
						${data.date!'Now'}
					</span>

					<span class="widget-toolbar">
						<a href="#" data-action="collapse">
							<i class="ace-icon fa fa-chevron-up"></i>
						</a>
					</span>
				</div>

				<div class="widget-body">
					<div class="widget-main">
						${data.comment!} <br />
						${data.activityName!}
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
	</div><!-- /.timeline-items -->
</div><!-- /.timeline-container -->

<script type="text/javascript">
$(document).ready(function(){
  $('.timeline-items').ace_scroll({
      size: 380
  });
});

</script>
</#if>
