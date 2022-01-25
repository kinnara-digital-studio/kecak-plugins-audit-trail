package com.kinnara.kecakplugins.audittrail;

import com.kinnara.kecakplugins.audittrail.model.AuditTrailModel;
import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.directory.model.User;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.sql.Blob;
import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kinnara.kecakplugins.audittrail.AuditTrailMonitoringMultirowFormBinder.Fields.*;

public class AuditTrailAceFormElement extends Element implements FormBuilderPaletteElement{

	@Override
	public String getFormBuilderTemplate() {
		return "<label class='label'>" + getLabel() + "</label>";
	}

	@Override
	public String getLabel() {
		return getName();
	}

	@Override
	public String getClassName() {
		return this.getClass().getName();
	}

	@Override
	public String getPropertyOptions() {
        final List<Map<String, String>> workflowVariableOptions = AuditTrailUtil.getWorkflowVariableOptions();

        final String[] args = {
                AuditTrailMonitoringMultirowFormBinder.class.getName(),
                new JSONArray(workflowVariableOptions).toString().replaceAll("\"", "'")
        };
        return AppUtil.readPluginResource(getClass().getName(), "/properties/AuditTrailAceFormElement.json", args, true, "/messages/AuditTrailAceFormElement");
	}

	@Override
	public String getName() {
		return "Audit Trail Ace Element";
	}

	@Override
	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	@Override
	public String getDescription() {
		return getClass().getPackage().getImplementationTitle();
	}

	@Override
	public String getFormBuilderCategory() {
		return "Kecak";
	}

	@Override
	public int getFormBuilderPosition() {
		return 100;
	}

	@Override
	public String getFormBuilderIcon() {
		return "/plugin/org.joget.apps.form.lib.TextField/images/textField_icon.gif";
	}

	protected String renderTemplate(String template,FormData formData, Map dataModel) {
		FormUtil.setReadOnlyProperty(this);
		
		boolean isHidden = ("true".equals(getPropertyString("hiddenWhenReadonly"))
                && FormUtil.isReadonly(this.getParent(), formData)) ||
                ("true".equals(getPropertyString("hiddenWhenAsSubform"))
                        && FormUtil.findRootForm(this) != null
                        && FormUtil.findRootForm(this).getParent() != null);
        dataModel.put("hidden", isHidden);

        if(isHidden) {
            return "";
        }

        dataModel.put("className", getClassName());

        // Data tables datas container
        final List<String> headers = new ArrayList<>();

        // Column id container
        final List<String> columnList = new ArrayList<>();
        final Map<String, String>[] columnProperties = getColumnProperties();
        if (columnProperties != null && columnProperties.length > 0) {
            for (Map<String, String> headerProp : columnProperties) {
                columnList.add(getField(headerProp));
                headers.add(String.valueOf(headerProp.get("columnLabel")));
            }
            dataModel.put("headers", headers);
        }

        final List<AuditTrailModel> data = new ArrayList<>();
        final FormRowSet rowSet = getTimelineData(this, formData);
        
        ApplicationContext context = AppUtil.getApplicationContext();
		WorkflowManager workflowManager = (WorkflowManager) context.getBean("workflowManager");
		
		
        for (FormRow row : rowSet) {
            final AuditTrailModel audit = new AuditTrailModel();

            audit.setId(row.getId());
            audit.setPerformer(row.getProperty(USER_FULLNAME.toString()));
            audit.setDate(row.getProperty(FINISH_TIME.toString()));
            audit.setComment(row.getProperty(getPropertyVariableNote()));
            
            WorkflowAssignment workflowAssignment = workflowManager.getAssignment(row.getProperty(ID.toString()));
            if(workflowAssignment!=null) {
            	Collection<WorkflowVariable> variableList = workflowManager.getActivityVariableList(workflowAssignment.getActivityId());
            	String serviceLabel = "";
            	for(WorkflowVariable wVar: variableList) {
        			if(wVar.getName().equals("serviceLabel")) {
        				serviceLabel = (String) wVar.getVal();
        			}
        		}
            	audit.setProcessName(serviceLabel);
            }else {
            	audit.setProcessName(row.getProperty(PROCESS_NAME.toString()));
            }
            

            String avatarUri = getAvatarUri(row.getProperty(USERNAME.toString()));
            audit.setAvatar(avatarUri);

            data.add(audit);
        }

        dataModel.put("datas", data);

        Object[] sortBy = (Object[]) getProperty("sortBy");
        if (sortBy != null && sortBy.length > 0) {
            dataModel.put("sort", translateSoryBy(sortBy));
        }

        dataModel.put("error", false);

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
	}
	
	public String formatColumn(String columnName, Map<String, String> properties, String recordId, String value, String appId, Long appVersion, String contextPath) {
        return value;
    }
	
	public String getField(Map<String, String> map) {
        return map.get("columnId");
    }
	
	public Map<String, String>[] getColumnProperties() {
        Map<String, Object> loadBinder = (Map<String, Object>) getProperty("loadBinder");

        return Optional.of(AuditTrailMonitoringMultirowFormBinder.class.getName().equals(loadBinder.get("className")) ? "monitoringColumns" : "columns")
                .map(this::getProperty)
                .map(o -> (Object[]) o)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(o -> (Map<String, String>) o)
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    r.forEach((k, v) -> m.put(String.valueOf(k), v));
                    return m;
                })
                .toArray((IntFunction<Map<String, String>[]>) Map[]::new);
    }
	
	private List<Map<String, String>> translateSoryBy(Object[] sortBy) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (Object o : sortBy) {
            result.add((Map<String, String>) o);
        }
        return result;
    }
	
	@Override
    public String renderAceTemplate(FormData formData, Map dataModel) {
        String template = "AuditTrailAceFormElement.ftl";
        return renderTemplate(template,formData,dataModel);
    }

    @Override
    public String renderAdminLteTemplate(FormData formData, Map dataModel){
        String template = "UnsupportedFormElement.ftl";
        return FormUtil.generateElementHtml(this, formData, template, dataModel);
    }

	@Override
	public String renderAdminKitTemplate(FormData formData, Map dataModel) {
        String template = "UnsupportedFormElement.ftl";
        return FormUtil.generateElementHtml(this, formData, template, dataModel);
	}

	@Override
	public String renderTemplate(FormData formData, Map dataModel) {
		String template = "UnsupportedFormElement.ftl";
        return FormUtil.generateElementHtml(this, formData, template, dataModel);
	}

    @Override
    public Object handleElementValueResponse(@Nonnull Element element, @Nonnull FormData formData) throws JSONException {
        final FormRowSet timelineData = getTimelineData((AuditTrailAceFormElement) element, formData);
        return timelineData.stream()
                .map(Try.onFunction(JSONObject::new))
                .collect(JSONCollectors.toJSONArray());
    }

    @Nonnull
    protected FormRowSet getTimelineData(AuditTrailAceFormElement element, FormData formData) {
        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        final String appId = appDefinition.getAppId();
        final long appVersion = appDefinition.getVersion();
        final String variableNote = element.getPropertyVariableNote();
        final FormRowSet rowSet = Optional.ofNullable(formData.getLoadBinderData(element))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .peek(row -> {
                    final String rowId = row.getId();

                    final String userFullname = row.getProperty(USER_FULLNAME.toString());
                    final String formattedUserFullname = formatColumn(USER_FULLNAME.toString(), null, rowId, userFullname, appId, appVersion, "");
                    if(formattedUserFullname != null) {
                        row.setProperty(USER_FULLNAME.toString(), formattedUserFullname);
                    }

                    final String finishTime = row.getProperty(FINISH_TIME.toString());
                    final String formatterFinishTime = formatColumn(FINISH_TIME.toString(), null, rowId, finishTime, appId, appVersion, "");
                    if(formatterFinishTime != null) {
                        row.setProperty(FINISH_TIME.toString(), formatterFinishTime);
                    }

                    final String statusTimeline = row.getProperty(variableNote);
                    final String formattedStatusTimeline = formatColumn(variableNote, null, rowId, statusTimeline, appId, appVersion, "");
                    if(formattedStatusTimeline != null) {
                        row.getProperty(variableNote, formattedStatusTimeline);
                    }
                })
                .collect(FormRowSet::new, FormRowSet::add, FormRowSet::addAll);

        return rowSet;
    }

    protected String getAvatarUri(String username) {
        final ApplicationContext appContext = AppUtil.getApplicationContext();
        final ExtDirectoryManager directoryManager = (ExtDirectoryManager) appContext.getBean("directoryManager");
        final Optional<User> optUser = Optional.of(username).map(directoryManager::getUserByUsername);
        final Optional<Blob> optProfilePicture = optUser.map(User::getProfilePicture);

        long blobLength = optProfilePicture
                .map(Try.onFunction(Blob::length))
                .orElse(0L);

        return optProfilePicture
                .map(Try.onFunction(b -> b.getBytes(1, (int) blobLength)))
                .map(b -> Base64.getEncoder().encodeToString(b))

                // get profile picture in base64 format
                .map(s -> "data:image/jpeg;base64," + s)

                // get default profile picture
                .orElseGet(() -> AppUtil.getRequestContextPath() + "/images/default-avatar.png");
    }

    protected String getPropertyVariableNote() {
        return getPropertyString("variableNote");
    }
}
