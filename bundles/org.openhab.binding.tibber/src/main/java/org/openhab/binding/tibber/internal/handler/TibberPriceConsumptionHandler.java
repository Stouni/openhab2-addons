/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.tibber.internal.handler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * The {@link TibberPriceConsumptionHandler} class contains fields mapping price info parameters.
 *
 * @author Stian Kjoglum - Initial contribution
 */
public class TibberPriceConsumptionHandler {
    public String query = "{\"query\": \"{viewer {homes {currentSubscription {priceInfo {current {total startsAt }}}}}}\"}";
    public String dailyquery = "{\"query\": \"{viewer {homes {daily: consumption(resolution: DAILY, last: 1) {nodes {from to cost unitPrice consumption consumptionUnit}}}}}\"}";
    public String hourlyquery = "{\"query\": \"{viewer {homes {hourly: consumption(resolution: HOURLY, last: 1) {nodes {from to cost unitPrice consumption consumptionUnit}}}}}\"}";

    public InputStream getInputStream() {
        InputStream myInputStream = new ByteArrayInputStream(query.getBytes(Charset.forName("UTF-8")));
        return myInputStream;
    }

    public InputStream getDailyInputStream() {
        InputStream myInputStream = new ByteArrayInputStream(dailyquery.getBytes(Charset.forName("UTF-8")));
        return myInputStream;
    }

    public InputStream getHourlyInputStream() {
        InputStream myInputStream = new ByteArrayInputStream(hourlyquery.getBytes(Charset.forName("UTF-8")));
        return myInputStream;
    }
}
