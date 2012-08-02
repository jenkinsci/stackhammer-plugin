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
package org.cloudsmith.jenkins.stackhammer.validation;

import hudson.Functions;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.cloudsmith.jenkins.stackhammer.common.StackOpResult;
import org.cloudsmith.stackhammer.api.model.Repository;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 999)
public class ValidationResult extends StackOpResult<String> {
	private static final long serialVersionUID = 264848698476660935L;

	private final AbstractBuild<?, ?> build;

	public ValidationResult(AbstractBuild<?, ?> build) {
		this.build = build;
	}

	@Override
	public ValidationResult clone() {
		ValidationResult clone;
		try {
			clone = (ValidationResult) super.clone();
		}
		catch(CloneNotSupportedException e) {
			throw new RuntimeException("Error cloning ValidationResult", e);
		}
		return clone;
	}

	/**
	 * Method called by the Stapler dispatcher. It is automatically detected
	 * when the dispatcher looks for methods that starts with &quot;do&quot;
	 * The method doValidation corresponds to the path <build>/stackhammerValidation/dependencyGraph
	 * 
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doDependencyGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
		String name = req.getRestOfPath();
		String result = getResult();
		if((name == null || name.isEmpty()) && result != null) {
			rsp.setContentType("image/svg+xml");
			OutputStream out = rsp.getOutputStream();
			try {
				byte[] svgData = Base64.decodeBase64(result);
				// svgData = GraphTrimmer.stripFixedSize(svgData);
				rsp.setContentLength(svgData.length);
				out.write(svgData);
				return;
			}
			finally {
				out.close();
			}
		}
		rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	public String getDisplayName() {
		return "Validation Result";
	}

	@Override
	public String getIconFileName() {
		return Functions.getResourcePath() + "/plugin/stackhammer/icons/hammer-32x32.png";
	}

	@Override
	public String getLargeIconFileName() {
		return "/plugin/stackhammer/icons/hammer-48x48.png";
	}

	public String getStackBase() {
		Repository repo = getRepository();
		return repo == null
				? null
				: repo.getProvider().getRepositoryBase(repo.getOwner(), repo.getName(), repo.getBranch());
	}

	public String getSummary() {
		return getSummary(getResultDiagnostics());
	}

	public String getSummaryValidationGraphURL() {
		return getResult() != null
				? getUrlFor("dependencyGraph")
				: null;
	}

	@Override
	public String getUrlName() {
		return "validationReport";
	}

	public String getValidationGraphURL() {
		return getResult() != null
				? "dependencyGraph"
				: null;
	}
}
