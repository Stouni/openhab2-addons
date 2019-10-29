# Tibber Binding

The Tibber Binding connects to the [Tibber API](https://developer.tibber.com), and use queries at frequent polls to retrieve price and consumption information for users with Tibber subscription, and provides additional live measurements via websocket for users having Tibber Pulse. 

Refresh time (poll frequency) is set manually as part of setup, minimum 1 minute.

If using Tibber Pulse, Pulse has to be chosen as addon from dropdown list during setup.

## Supported Things

Provided one have Tibber Pulse and/or Tibber Account, the Tibber API is recognized as a thing in OpenHab. The channels (i.e. measurements) associated with the Tibber binding: 

Tibber Account:

* Home Id:              Needed to recognise Thing
* Current Total:        Current Total Price (energy + tax)
* Starts At:            Current Price Timestamp
* Daily Cost:           Daily Cost (last/previous day)
* Daily Consumption:    Daily Consumption (last/previous day)
* Dail From:            Timestamp (daily from)
* Daily To:             Timestamp (daily to)
* Hourly Cost:          Hourly Cost (last/previous hour)
* Hourly Consumption:   Hourly Consumption (last/previous hour)
* Hourly From:          Timestamp (hourly from)
* Hourly To:            TimeStamp (hourly to)

Tibber Pulse:

* Timestamp:                Timestamp for live measurements
* Power:                    Live Power Consumption
* Last Meter Consumption:   Last Recorded Meter Consumption
* Accumulated Consumption:  Accumulated Consumption since Midnight
* Accumulated Cost:         Accumulated Cost since Midnight
* Currency:                 Currency of Cost
* Min Power:                Min Power Consumption since Midnight
* Average Power:            Average Power Consumption since Midnight
* Max Power:                Max Power Consumption since Midnight


## Binding Configuration

To access and initiate the Tibber binding, a Tibber account is required.

Required input needed for initialization:

* Tibber token
* Refresh Interval (min 1 minute)
* Addons (Pulse or None)

Note: Tibber token is retrieved from your Tibber account:
[Tibber Account](https://developer.tibber.com/settings/accesstoken)


## Thing Configuration

When Tibber binding is installed, Tibber API should be autodiscovered in PaperUI. Retrieve personal token from above link, and initialize/start binding from PaperUI. Tibber API will be autodiscovered if provided input is correct.

Note: 
Gson is required. If not able to initialize binding, perform from OpenHab console:

```
bundle:install http://central.maven.org/maven2/com/google/code/gson/gson/2.8.5/gson-2.8.5.jar
```


## Full Example

demo.items:

```
String                 TibberAPIId                           "Home"                       {channel="tibber:tibberapi:3b1e9219:id"}
Number:Dimensionless   TibberAPICurrentTotal                 "Current total price"        {channel="tibber:tibberapi:3b1e9219:current_total"}
String                 TibberAPICurrentStartsAt              "Timestamp current"          {channel="tibber:tibberapi:3b1e9219:current_startsAt"}
String                 TibberAPIDailyFrom                    "Timestamp daily from"       {channel="tibber:tibberapi:3b1e9219:daily_from"}
String                 TibberAPIDailyTo                      "Timestamp daily to"         {channel="tibber:tibberapi:3b1e9219:daily_to"}
Number:Dimensionless   TibberAPIDailyCost                    "Total daily cost"           {channel="tibber:tibberapi:3b1e9219:daily_cost"}
Number:Dimensionless   TibberAPIDailyConsumption             "Total daily consumption"    {channel="tibber:tibberapi:3b1e9219:daily_consumption"}
String                 TibberAPIHourlyFrom                   "Timestamp hourly from"      {channel="tibber:tibberapi:3b1e9219:hourly_from"}
String                 TibberAPIHourlyTo                     "Timestamp hourly to"        {channel="tibber:tibberapi:3b1e9219:hourly_to"}
Number:Dimensionless   TibberAPIHourlyCost                   "Total hourly cost"          {channel="tibber:tibberapi:3b1e9219:hourly_cost"}
Number:Dimensionless   TibberAPIHourlyConsumption            "Total hourly consumption"   {channel="tibber:tibberapi:3b1e9219:hourly_consumption"}
String                 TibberAPILiveTimestap                 "Live timestamp"             {channel="tibber:tibberapi:3b1e9219:live_timestap"}
Number:Dimensionless   TibberAPILivePower                    "Live consumption"           {channel="tibber:tibberapi:3b1e9219:live_power"}
Number:Dimensionless   TibberAPILiveLastMeterConsumption     "Last meter consumption"     {channel="tibber:tibberapi:3b1e9219:live_lastMeterConsumption"}
Number:Dimensionless   TibberAPILiveAccumulatedConsumption   "Accumulated consumption"    {channel="tibber:tibberapi:3b1e9219:live_accumulatedConsumption"}
Number:Dimensionless   TibberAPILiveAccumulatedCost          "Accumulated cost"           {channel="tibber:tibberapi:3b1e9219:live_accumulatedCost"}
String                 TibberAPILiveCurrency                 "Currency"                   {channel="tibber:tibberapi:3b1e9219:live_currency"}
Number:Dimensionless   TibberAPILiveMinPower                 "Min consumption"            {channel="tibber:tibberapi:3b1e9219:live_minPower"}
Number:Dimensionless   TibberAPILiveAveragePower             "Average consumption"        {channel="tibber:tibberapi:3b1e9219:live_averagePower"}
Number:Dimensionless   TibberAPILiveMaxPower                 "Max consumption"            {channel="tibber:tibberapi:3b1e9219:live_maxPower"}
```
