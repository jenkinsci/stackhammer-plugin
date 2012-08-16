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

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.Builder;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;

import org.cloudsmith.jenkins.stackhammer.validation.ValidationDescriptor;
import org.cloudsmith.jenkins.stackhammer.validation.Validator;
import org.cloudsmith.stackhammer.api.Constants;
import org.cloudsmith.stackhammer.api.StackHammerModule;
import org.cloudsmith.stackhammer.api.model.Diagnostic;
import org.cloudsmith.stackhammer.api.model.LogEntry;
import org.cloudsmith.stackhammer.api.model.PollResult;
import org.cloudsmith.stackhammer.api.model.Provider;
import org.cloudsmith.stackhammer.api.model.Repository;
import org.cloudsmith.stackhammer.api.model.ResultWithDiagnostic;
import org.cloudsmith.stackhammer.api.service.RepositoryService;
import org.cloudsmith.stackhammer.api.service.StackHammerFactory;
import org.cloudsmith.stackhammer.api.service.StackService;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * A {@link Builder} that runs Stack Hammer Tests
 */
public class TestRunner extends Builder {

	private final String branch;

	private final String stack;

	private final String testNames;

	private final Boolean undeploy;

	private final String apiKey;

	@DataBoundConstructor
	public TestRunner(String stack, String branch, String testNames, Boolean undeploy, String apiKey) {
		this.stack = stack;
		this.branch = branch;
		this.testNames = testNames;
		this.undeploy = undeploy;
		this.apiKey = apiKey;
	}

	private void emitLogEntries(List<LogEntry> logEntries, PrintStream logger) {
		for(LogEntry logEntry : logEntries)
			logger.println(logEntry);
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getBranch() {
		return branch;
	}

	@Override
	public TestRunnerDescriptor getDescriptor() {
		return (TestRunnerDescriptor) super.getDescriptor();
	}

	public String getStack() {
		return stack;
	}

	public String getTestNames() {
		return testNames;
	}

	public Boolean getUndeploy() {
		return undeploy;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
		try {
			PrintStream logger = listener.getLogger();

			ValidationDescriptor validationDesc = (ValidationDescriptor) Jenkins.getInstance().getDescriptorOrDie(
				Validator.class);

			String serverURL = validationDesc.getServiceURL();
			URI uri = URI.create(serverURL);
			logger.format(
				"Using parameters%n scheme=%s%n host=%s%n port=%s%n prefix=%s%n", uri.getScheme(), uri.getHost(),
				uri.getPort(), uri.getPath());

			Injector injector = Guice.createInjector(new StackHammerModule(
				uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), getApiKey()));

			StackHammerFactory factory = injector.getInstance(StackHammerFactory.class);
			RepositoryService repoService = factory.createRepositoryService();
			String[] splitName = getStack().split("/");
			String owner = splitName[0];
			String name = splitName[1];
			logger.format(
				"Verifying that a local clone of repository %s/%s[%s] exists at Stack Hammer Service%n", owner, name,
				branch);
			ResultWithDiagnostic<Repository> cloneResult = repoService.cloneRepository(
				Provider.GITHUB, owner, name, branch);

			if(cloneResult.getSeverity() == Diagnostic.ERROR) {
				listener.error(cloneResult.toString());
				return false;
			}
			cloneResult.log(logger);

			StackService stackService = factory.createStackService();
			Repository repo = cloneResult.getResult();

			long pollInterval = validationDesc.getPollInterval().longValue();
			long startTime = System.currentTimeMillis();
			long lastPollTime = startTime;
			long failTime = Long.MAX_VALUE;
			Integer maxTimeObj = validationDesc.getMaxTime();
			if(maxTimeObj != null && maxTimeObj.longValue() > 0)
				failTime = startTime + maxTimeObj.longValue() * 1000;

			List<String> testNameList = null;
			if(testNames != null) {
				int start = 0;
				int idx = testNames.indexOf(',');
				int len = testNames.length();
				if(idx < 0)
					idx = len;

				while(start < idx) {
					String testName = testNames.substring(start, idx).trim();
					if(!testName.isEmpty()) {
						if(testNameList == null)
							testNameList = new ArrayList<String>();
						testNameList.add(testName);
					}

					start = idx + 1;
					if(start < len) {
						idx = testNames.indexOf(',', start);
						if(idx < 0)
							idx = len;
					}
				}
			}
			Boolean undeployObj = getUndeploy();
			boolean undeploy = undeployObj == null
					? false
					: undeployObj.booleanValue();

			logger.format("Sending order to run tests on stack %s/%s to Stack Hammer Service%n", owner, name);
			String jobIdentifier = stackService.runTests(
				repo, repo.getOwner() + "/" + repo.getName(), testNameList, undeploy);

			for(;;) {
				long now = System.currentTimeMillis();
				if(now > failTime) {
					logger.format("Job didn't finish in time.%n");
					return false;
				}

				long sleepTime = (lastPollTime + pollInterval * 1000) - now;
				if(sleepTime > 0)
					Thread.sleep(sleepTime);

				lastPollTime = System.currentTimeMillis();
				PollResult pollResult = stackService.pollJob(jobIdentifier);
				switch(pollResult.getJobState()) {
					case SCHEDULED:
					case STARTING:
						continue;
					case SLEEPING:
					case RUNNING:
						emitLogEntries(pollResult.getLogEntries(), logger);
						continue;
					case CANCELLED:
						listener.error("Job was cancelled");
						return false;
					default:
						emitLogEntries(pollResult.getLogEntries(), logger);
						break;
				}
				break;
			}

			ResultWithDiagnostic<String> runTestsResult = stackService.getRunTestsResult(jobIdentifier);
			if(runTestsResult == null) {
				listener.error("No result was returned from test run");
				return false;
			}

			if(runTestsResult.getSeverity() == Diagnostic.ERROR) {
				// The error has already been logged, so don't repeat it
				return false;
			}

			String junitXML = runTestsResult.getResult();
			File tmpFile = File.createTempFile("testresult-", ".xml", build.getRootDir());
			try {
				Writer writer = new OutputStreamWriter(new FileOutputStream(tmpFile), Constants.UTF_8);
				try {
					writer.write(junitXML);
				}
				finally {
					writer.close();
				}

				TestResult testResult = new TestResult();
				testResult.parse(tmpFile);
				TestResultAction data = new TestResultAction(build, testResult, listener);
				build.addAction(data);
			}
			finally {
				tmpFile.delete();
			}
		}
		catch(Exception e) {
			e.printStackTrace(listener.error("Exception during deployment of %s", getStack()));
			return false;
		}
		return true;
	}
}
