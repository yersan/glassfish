/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.web.admin.cli;

import java.beans.PropertyVetoException;

import com.sun.enterprise.admin.util.Target;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.grizzly.config.dom.NetworkConfig;
import com.sun.grizzly.config.dom.Transport;
import com.sun.grizzly.config.dom.Transports;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Command to create transport element within network-config
 *
 * Sample Usage : create-transport [--acceptorThreads no_of_acceptor_threads] [--bufferSizeBytes buff_size_bytes]
 * [--classname class_name] [--enableSnoop true|false][--selectionKeyHandler true|false] [--displayConfiguration
 * true|false][--maxConnectionsCount count] [--idleKeyTimeoutSeconds idle_key_timeout] [--tcpNoDelay true|false]
 * [--readTimeoutMillis read_timeout][--writeTimeoutMillis write_timeout] [--byteBufferType buff_type]
 * [--selectorPollTimeoutMillis true|false] transport_name
 *
 * domain.xml element example <transports> <transport name="tcp" /> </transports>
 *
 * @author Nandini Ektare
 */
@Service(name = "create-transport")
@Scoped(PerLookup.class)
@I18n("create.transport")
@Cluster({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class CreateTransport implements AdminCommand {
    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(CreateTransport.class);
    @Param(name = "transportname", primary = true)
    String transportName;
    @Param(name = "acceptorthreads", optional = true, defaultValue = "-1")
    String acceptorThreads;
    @Param(name = "buffersizebytes", optional = true, defaultValue = "8192")
    String bufferSizeBytes;
    @Param(name = "bytebuffertype", optional = true, defaultValue = "HEAP")
    String byteBufferType;
    @Param(name = "classname", optional = true,
        defaultValue = "com.sun.grizzly.TCPSelectorHandler")
    String className;
    @Param(name = "displayconfiguration", optional = true, defaultValue = "false")
    Boolean displayConfiguration;
    @Param(name = "enablesnoop", optional = true, defaultValue = "false")
    Boolean enableSnoop;
    @Param(name = "idlekeytimeoutseconds", optional = true, defaultValue = "30")
    String idleKeyTimeoutSeconds;
    @Param(name = "maxconnectionscount", optional = true, defaultValue = "4096")
    String maxConnectionsCount;
    @Param(name = "readtimeoutmillis", optional = true, defaultValue = "30000")
    String readTimeoutMillis;
    @Param(name = "writetimeoutmillis", optional = true, defaultValue = "30000")
    String writeTimeoutMillis;
    @Param(name = "selectionkeyhandler", optional = true)
    String selectionKeyHandler;
    @Param(name = "selectorpolltimeoutmillis", optional = true, defaultValue = "1000")
    String selectorPollTimeoutMillis;
    @Param(name = "tcpnodelay", optional = true, defaultValue = "false")
    Boolean tcpNoDelay;
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;
    @Inject(name = ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    @Inject
    Domain domain;
    @Inject
    Habitat habitat;

    /**
     * Executes the command with the command parameters passed as Properties where the keys are the paramter names and
     * the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        Target targetUtil = habitat.getComponent(Target.class);
        Config newConfig = targetUtil.getConfig(target);
        if (newConfig!=null) {
            config = newConfig;
        }
        final ActionReport report = context.getActionReport();
        // check for duplicates
        NetworkConfig networkConfig = config.getNetworkConfig();
        Transports transports = networkConfig.getTransports();
        for (Transport transport : transports.getTransport()) {
            if (transportName != null &&
                transportName.equalsIgnoreCase(transport.getName())) {
                report.setMessage(localStrings.getLocalString(
                    "create.transport.fail.duplicate",
                    "{0} transport already exists. Cannot add duplicate transport"));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        // Add to the <network-config>
        try {
            ConfigSupport.apply(new SingleConfigCode<Transports>() {
                public Object run(Transports param)
                    throws PropertyVetoException, TransactionFailure {
                    boolean docrootAdded = false;
                    boolean accessLogAdded = false;
                    Transport newTransport = param.createChild(Transport.class);
                    newTransport.setName(transportName);
                    newTransport.setAcceptorThreads(acceptorThreads);
                    newTransport.setBufferSizeBytes(bufferSizeBytes);
                    newTransport.setByteBufferType(byteBufferType);
                    newTransport.setClassname(className);
                    newTransport.setDisplayConfiguration(displayConfiguration.toString());
                    newTransport.setEnableSnoop(enableSnoop.toString());
                    newTransport.setIdleKeyTimeoutSeconds(idleKeyTimeoutSeconds);
                    newTransport.setMaxConnectionsCount(maxConnectionsCount);
                    newTransport.setName(transportName);
                    newTransport.setReadTimeoutMillis(readTimeoutMillis);
                    newTransport.setSelectionKeyHandler(selectionKeyHandler);
                    newTransport.setSelectorPollTimeoutMillis(
                        selectorPollTimeoutMillis);
                    newTransport.setWriteTimeoutMillis(writeTimeoutMillis);
                    newTransport.setTcpNoDelay(tcpNoDelay.toString());
                    param.getTransport().add(newTransport);
                    return newTransport;
                }
            }, transports);
        } catch (TransactionFailure e) {
            report.setMessage(
                localStrings.getLocalString("create.transport.fail",
                    "Failed to create transport {0} ", transportName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
