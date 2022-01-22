package com.kinnara.kecakplugins.audittrail.optionsbinder.generator;

import org.joget.apps.form.model.FormRow;

public interface OptionsGenerator {
	void generate(OnGenerateListener listener);

	@FunctionalInterface
	interface OnGenerateListener {
		void onGenerated(FormRow row);
	}
}
