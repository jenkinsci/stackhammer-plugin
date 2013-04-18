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

import hudson.Functions;
import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.Api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.cloudsmith.geppetto.diagnostic.Diagnostic;
import org.cloudsmith.stackhammer.api.model.MessageWithSeverity;
import org.cloudsmith.stackhammer.api.model.Repository;
import org.cloudsmith.stackhammer.api.model.ResultWithDiagnostic;

public abstract class StackOpResult<T> implements Action, Serializable, Cloneable {
	private static final long serialVersionUID = 264848698476660935L;

	private ResultWithDiagnostic<Repository> cloneDiagnostic;

	private ResultWithDiagnostic<T> resultDiagnostic;

	private final AbstractBuild<?, ?> build;

	protected StackOpResult(AbstractBuild<?, ?> build) {
		this.build = build;
	}

	public Api getApi() {
		return new Api(this);
	}

	public AbstractBuild<?, ?> getBuild() {
		return build;
	}

	public List<Diagnostic> getCloneDiagnostics() {
		return cloneDiagnostic == null
				? Collections.<Diagnostic> emptyList()
				: cloneDiagnostic.getChildren();
	}

	public String getIconFileName() {
		return Functions.getResourcePath() + "/plugin/stackhammer/icons/hammer-32x32.png";
	}

	public String getLargeIconFileName() {
		return "/plugin/stackhammer/icons/hammer-48x48.png";
	}

	protected Repository getRepository() {
		return cloneDiagnostic == null
				? null
				: cloneDiagnostic.getResult();
	}

	public T getResult() {
		return resultDiagnostic == null
				? null
				: resultDiagnostic.getResult();
	}

	public List<Diagnostic> getResultDiagnostics() {
		return resultDiagnostic == null
				? Collections.<Diagnostic> emptyList()
				: resultDiagnostic.getChildren();
	}

	public int getResultDiagnosticsCount() {
		return getResultDiagnostics().size();
	}

	protected String getSummary(List<? extends Diagnostic> messages) {
		int errorCount = 0;
		int warningCount = 0;
		for(Diagnostic msg : messages) {
			switch(msg.getSeverity()) {
				case MessageWithSeverity.WARNING:
					warningCount++;
					break;
				case MessageWithSeverity.ERROR:
				case MessageWithSeverity.FATAL:
					errorCount++;
			}
		}

		if(errorCount == 0) {
			if(warningCount == 0)
				return "No errors or warnings";
			return warningCount + (warningCount > 1
					? " warnings"
					: " warning");
		}

		if(warningCount == 0)
			return errorCount + (errorCount > 1
					? " errors"
					: " error");
		return errorCount + (errorCount > 1
				? " errors and "
				: " error and ") + warningCount + (warningCount > 1
				? " warnings"
				: " warning");
	}

	public abstract String getTitle();

	protected String getUrlFor(String item) {
		return getUrlName() + '/' + item;
	}

	/**
	 * @param cloneDiagnostic the cloneDiagnostic to set
	 */
	public void setCloneDiagnostic(ResultWithDiagnostic<Repository> cloneDiagnostic) {
		this.cloneDiagnostic = cloneDiagnostic;
	}

	public void setResult(ResultWithDiagnostic<T> resultDiagnostic) {
		this.resultDiagnostic = resultDiagnostic;
	}
}
