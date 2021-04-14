package io.github.adobe.sign.impl.workflow;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import io.github.adobe.sign.core.actions.SignAction;
import io.github.adobe.sign.core.auth.CredentialLoader;
import io.github.adobe.sign.impl.CliCommand;
import io.github.adobe.sign.impl.CliComponent;
import io.github.adobe.sign.impl.actions.AgreementSendAction;
import io.github.adobe.sign.impl.actions.TransientDocument;
import io.github.adobe.sign.impl.auth.BasicSignAuth;
import io.github.adobe.sign.impl.auth.FileCredentialLoader;
import io.github.adobe.sign.impl.executor.BasicWorkflowExecutor;

@CliComponent(name = "Simple Workflow")
public class SimpleWorkflow extends BasicWorkflowExecutor implements CliCommand {

    SignAction transientDocument;
    SignAction agreementSendAction;

    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption(Option.builder("c").hasArg().required().build());
        return options;
    }

    @Override
    public <T> T execute(Class<T> type, List<String> args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine cli = parser.parse(getOptions(), args.stream().toArray(String[]::new));
        CredentialLoader credentialLoader = new FileCredentialLoader(cli.getOptionValue("c"));
        this.transientDocument = new TransientDocument(credentialLoader);

        // Add the work item to the workflow
        this.addAction(this.transientDocument);

        this.agreementSendAction = new AgreementSendAction(credentialLoader);
        this.addAction(this.agreementSendAction);

        // Run it
        return type.cast(this.invoke(new BasicSignAuth(), null, null));

    }

}
