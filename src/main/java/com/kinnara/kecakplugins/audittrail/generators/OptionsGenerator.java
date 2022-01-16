package com.kinnara.kecakplugins.audittrail.generators;

import org.joget.apps.form.model.FormRow;

import java.util.function.Consumer;

public interface OptionsGenerator {
	void generate(Consumer<FormRow> listener);
}
