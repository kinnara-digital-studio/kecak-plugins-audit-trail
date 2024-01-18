package com.kinnarastudio.kecakplugins.audittrail.form;

import com.kinnarastudio.commons.Declutter;
import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryManager;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.Nonnull;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ElementValueHistoryField extends Element implements FormBuilderPaletteElement, Declutter {
    private final static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    /**
     * Executed when displaying element
     *
     * @param formData
     * @param dataModel
     * @return
     */
    @Override
    public String renderTemplate(FormData formData, final Map dataModel) {
        String template = "ElementValueHistoryField.ftl";

        dataModel.put("message", ifEmptyThen(getMessage(formData), getEmptyMessage(formData)));
        dataModel.put("className", getClassName());
        dataModel.put("elementLabel", getLabel());

        final FormRowSet options = getOptions();
        List<Map<String, String>> historyList = getHistory(this, formData)
                .stream()
                .map(r -> {
                    // get default column
                    Map<String, String> historyRow = options.stream()
                            .map(option -> option.getProperty("value"))
                            .filter(this::isNotEmpty)
                            .collect(TreeMap::new, (m, s) -> m.put(s, r.getProperty(s, "")), Map::putAll);

                    // get field column
                    getTargetElement().forEach(s -> historyRow.put(s, r.getProperty(s, "")));

                    return historyRow;
                })
                .collect(Collectors.toList());

        dataModel.put("isShowingHistoryList", !options.isEmpty());
        dataModel.put("historyList", historyList);
        dataModel.put("isHidden", getHidden());

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    @Override
    public String getName() {
        return getLabel();
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        return resourceBundle.getString("buildNumber");
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return "Value History";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        String[] args = new String[] {
                FormLoadMultiRowElementBinder.class.getName(),
                ElementValueHistoryMultirowLoadBinder.class.getName(),
                getDefaultMessage()
        };
        return AppUtil.readPluginResource(getClassName(), "/properties/form/ElementValueHistoryField.json", args, true, "/messages/form/ElementValueHistoryField").replaceAll("\"", "'");
    }

    /**
     * Get history data sorted by created date
     *
     * @param formData
     * @return
     */
    @Nonnull
    protected FormRowSet getHistory(Element element, FormData formData) {
        return Optional.ofNullable(formData)
                .map(fd -> fd.getLoadBinderData(element))
                .orElseGet(FormRowSet::new);
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

    @Override
    public String getFormBuilderTemplate() {
        return "<span class='form-floating-label'>"+getLabel()+"</span>";
    }

    /**
     * Executed when accessing element from API {DataJsonController}
     *
     * @param element
     * @param formData
     * @return
     * @throws JSONException
     */
    @Override
    public Object handleElementValueResponse(@Nonnull Element element, @Nonnull FormData formData) throws JSONException {
        if(element instanceof ElementValueHistoryField) {
            final FormRowSet history = ((ElementValueHistoryField)element).getHistory(element, formData);
            JSONObject jsonResponse = new JSONObject();

            jsonResponse.put("history", history.stream()
                    .map(Try.onFunction(JSONObject::new))
                    .collect(JSONCollectors.toJSONArray()));

            jsonResponse.put("message", ((ElementValueHistoryField)element).getMessage(formData));

            return jsonResponse;
        }

        return super.handleElementValueResponse(element, formData);
    }

    @Override
    public Boolean isReadonly(FormData formData) {
        return true;
    }

    protected String getMessage(FormData formData) {
        final DirectoryManager directoryManager = (DirectoryManager) AppUtil.getApplicationContext().getBean("directoryManager");

        final Form form = FormUtil.findRootForm(this);
        final FormRowSet historyValues = getHistory(this, formData);
        final FormRow firstRow = historyValues.stream().findFirst().orElse(null);
        if(firstRow == null) {
            return getPropertyString("emptyMessage");
        }

        final String message = getTargetElement()
                .stream()
                .map(s -> FormUtil.findElement(s, form, formData))
                .filter(Objects::nonNull)
                .map(element -> {
                    String text = Optional.of(getPropertyString("message", formData))
                            .map(String::trim)
                            .filter(this::isNotEmpty)
                            .orElseGet(this::getDefaultMessage);

                    String currentFieldId = element.getPropertyString("id");
                    text = text.replaceAll("\\{fieldId}", "<b>" + currentFieldId + "</b>");

                    String currentFieldName = Optional.of(element).map(e -> e.getPropertyString("label")).orElse(currentFieldId);
                    text = text.replaceAll("\\{fieldLabel}", "<b>" + currentFieldName + "</b>");

                    text = text.replaceAll("\\{changedAt}", "<b>" + Optional.of(firstRow).map(FormRow::getDateModified).map(Try.onFunction(dateFormat::format)).orElse("-") + "</b>");

                    String username = ifNullThen(firstRow.getCreatedBy(), "-");
                    text = text.replaceAll("\\{changedBy}", "<b>" + username + "</b>");

                    User user = Optional.of(username)
                            .map(directoryManager::getUserById)
                            .orElseGet(User::new);

                    text = text.replaceAll("\\{changedByFirstName}", "<b>" + ifNullThen(user.getFirstName(), "-") + "</b>");
                    text = text.replaceAll("\\{changedByLastName}", "<b>" + ifNullThen(user.getLastName(), "-") + "</b>");
                    text = text.replaceAll("\\{valueChangedTo}", "<b>" + ifNullThen(firstRow.getProperty(currentFieldId), "-") + "</b>");

                    String valueChangedFrom = historyValues.stream()
                            .skip(1)
                            .findFirst()
                            .map(r -> r.getProperty(currentFieldId))
                            .map(s -> "<strike>" + s + "</strike>")
                            .orElse(null);

                    text = text.replaceAll("\\{valueChangedFrom}", ifNullThen(valueChangedFrom, "-"));

                    return text;
                })
                .collect(Collectors.joining("<br/>"));

        return message;
    }


    protected String getDefaultMessage() {
        return "<i>{fieldLabel} has changed from {valueChangedFrom} to {valueChangedTo} at {changedAt} by {changedByFirstName} {changedByLastName}</i>";
    }

    protected String getPropertyString(String propertyName, FormData formData) {
        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        WorkflowAssignment workflowAssignment = Optional.of(formData)
                .map(FormData::getActivityId)
                .map(workflowManager::getAssignment)
                .orElse(null);
        return AppUtil.processHashVariable(getPropertyString(propertyName), workflowAssignment, null, null);
    }

    protected FormRowSet getOptions() {
        return Optional.of(getProperty("options"))
                .map(o -> (FormRowSet)o)
                .orElseGet(FormRowSet::new);
    }

    protected String getHidden(){
        return getPropertyString("is_hidden").split(";")[0];
    }

    protected String getEmptyMessage(FormData formData) {
        return getPropertyString("emptyMessage", formData);
    }

    protected Set<String> getTargetElement() {
        return Optional.of(getPropertyString("targetElement"))
                .map(s -> s.split("[;,]"))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .filter(this::isNotEmpty)
                .collect(Collectors.toSet());
    }
}
