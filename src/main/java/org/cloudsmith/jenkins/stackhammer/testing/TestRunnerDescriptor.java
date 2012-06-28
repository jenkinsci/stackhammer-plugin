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
package org.cloudsmith.jenkins.stackhammer.testing;

import hudson.Extension;
import hudson.tasks.Builder;

import org.cloudsmith.jenkins.stackhammer.common.StackOpDescriptor;

/**
 * Descriptor for {@link TestRunner}. Used as a singleton.
 * The class is marked as public so that it can be accessed from views.
 * 
 * <p>
 * See <tt>src/main/resources/com/cloudsmith/hammer/jenkins/deployment/Deployer/*.jelly</tt>
 * for the actual HTML fragment for the configuration screen.
 */
@Extension
public final class TestRunnerDescriptor extends StackOpDescriptor<Builder> {
	public TestRunnerDescriptor() {
		super(TestRunner.class);
	}

	/**
	 * This human readable name is used in the configuration screen.
	 */
	@Override
	public String getDisplayName() {
		return "Stack Hammer Testing";
	}
}
