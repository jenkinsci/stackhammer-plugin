/**
 * Copyright 2012-, Cloudsmith Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.cloudsmith.jenkins.stackhammer.common;

import hudson.model.Describable;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;

public abstract class StackOpDescriptor<T extends BuildStep & Describable<T>> extends BuildStepDescriptor<T> {

	public static String getParameter(JSONObject formData, String attributeName) {
		String value = formData.getString(attributeName);
		if(value == null)
			return null;

		value = value.trim();
		return value.isEmpty()
				? null
				: value;
	}

	public StackOpDescriptor(Class<? extends T> builderClass) {
		super(builderClass);
	}

	/**
	 * Performs on-the-fly validation of the form field 'stack'.
	 * 
	 * @param value
	 *        This parameter receives the value that the user has typed.
	 * @return
	 *         Indicates the outcome of the validation. This is sent to the browser.
	 */
	public FormValidation doCheckStack(@QueryParameter String value) throws IOException, ServletException {
		if(value.length() == 0)
			return FormValidation.error("Please specify a stack");
		String[] split = value.trim().split("/");
		if(split.length == 2) {
			String owner = split[0].trim();
			String name = split[1].trim();
			if(!(owner.isEmpty() || name.isEmpty())) {
				return FormValidation.ok();
			}
		}
		return FormValidation.error("Stack must be in the form <owner>/<name>");
	}

	/**
	 * Indicates that this builder can be used with all kinds of project types
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean isApplicable(Class<? extends AbstractProject> aClass) {
		return true;
	}
}
