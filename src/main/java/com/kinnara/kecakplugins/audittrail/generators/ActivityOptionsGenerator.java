package com.kinnara.kecakplugins.audittrail.generators;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PackageDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.service.FormUtil;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Helper to generate list of activities/tools/sub-processes/routes
 * @author aristo
 *
 */
public class ActivityOptionsGenerator implements OptionsGenerator {
	/**
	 * {@link WorkflowActivity}
	 */
	final private String type;

	public ActivityOptionsGenerator(String type) {
		this.type = type;
	}

	@Override
	public final void generate(Consumer<FormRow> listener) {
		ApplicationContext appContext = AppUtil.getApplicationContext();
		WorkflowManager wfManager = (WorkflowManager)appContext.getBean("workflowManager");

		// get published app or latest version
		final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
		final PackageDefinition packageDefinition = appDefinition.getPackageDefinition();
        long packageVersion = (packageDefinition != null) ? packageDefinition.getVersion() : 1L;
        final Collection<WorkflowProcess> processes = wfManager.getProcessList(appDefinition.getAppId(), String.valueOf(packageVersion));

		final Set<String> uniqueName = new HashSet<>();

		Optional.ofNullable(processes)
				.map(Collection::stream)
				.orElseGet(Stream::empty)
				.map(WorkflowProcess::getId)
				.map(wfManager::getProcessActivityDefinitionList)
				.flatMap(Collection::stream)
				.filter(activity -> type == null || "".equals(type) || activity.getType().equals(type))
				.filter(activity -> uniqueName.add(activity.getId()))
				.map(activity -> {
					FormRow row = new FormRow();
					row.put(FormUtil.PROPERTY_VALUE, activity.getId());
					row.put(FormUtil.PROPERTY_LABEL, activity.getName() + " ("+activity.getId()+")");
					return row;
				})
				.forEach(listener);
	}
}
