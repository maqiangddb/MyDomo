package mydomowebserver;

/*
 * #%L
 * MyDomoWebServer
 * %%
 * Copyright (C) 2011 - 2013 A. de Giuli
 * %%
 * This file is part of MyDomo done by A. de Giuli (arnaud.degiuli(at)free.fr).
 * 
 *     MyDomo is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     MyDomo is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with MyDomo.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.util.Hashtable;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private static final String MAIN_RESOURCES_URI = "/server";
	private static final String LIGHT_URI = "/light";
	private HttpService httpService = null;
	private static final Logger logger = Logger.getLogger(Activator.class
			.getName());
	private ServiceTracker<HttpService, Object> httpServiceTracker;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {

		System.setProperty("jetty.home.bundle", "MyDomoWebServer");

		// register the service
		context.registerService(LightRestService.class.getName(),
				new LightRestServiceImpl(), new Hashtable<String, String>());

		logger.info("STARTING HTTP SERVICE BUNDLE");

		httpServiceTracker = new ServiceTracker<HttpService, Object>(context,
				HttpService.class.getName(), null) {
			@Override
			public Object addingService(
					final ServiceReference<HttpService> serviceRef) {
				httpService = (HttpService) super.addingService(serviceRef);
				registerServlets();
				registerResources();
				return httpService;
			}

			@Override
			public void removedService(final ServiceReference<HttpService> ref,
					final Object service) {
				if (httpService == service) {
					unregisterServlets();
					unregisterResources();
					httpService = null;
				}
				super.removedService(ref, service);
			}

		};

		httpServiceTracker.open();

		logger.info("HTTP SERVICE BUNDLE STARTED");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {

		// close the service tracker
		this.httpServiceTracker.close();
		this.httpServiceTracker = null;
	}

	private void registerServlets() {
		try {
			httpService.registerServlet(LIGHT_URI, new LightServlet(), null,
					null);
		} catch (ServletException se) {
			throw new RuntimeException(se);
		} catch (NamespaceException se) {
			throw new RuntimeException(se);
		}
	}

	private void unregisterServlets() {
		httpService.unregister(LIGHT_URI);
	}

	private void registerResources() {
		try {
			httpService.registerResources(MAIN_RESOURCES_URI, "src/main/webapp", null);
		} catch (NamespaceException e) {
			throw new RuntimeException(e);
		}
	}

	private void unregisterResources() {
		httpService.unregister(MAIN_RESOURCES_URI);
	}
}
