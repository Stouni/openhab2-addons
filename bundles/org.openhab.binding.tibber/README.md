# Tibber Binding

The Tibber Binding connects to the [Tibber API](https://developer.tibber.com), and use queries at frequent polls to retrieve price and consumption information for users with Tibber subscription, and provides additional live measurements via websocket for users having Tibber Pulse. 

Refresh time (poll frequency) is set manually as part of setup, minimum 1 minute.

If using Tibber Pulse, Pulse will be recognized automatically by the binding if associated with HomeID used for setup, and live data stream will be initiated if Pulse verified present. 

## Supported Things

Provided one have Tibber Account, the Tibber API is recognized as a thing in OpenHab using the Tibber binding. Tibber Pulse is optional, but will enable live measurements. The channels (i.e. measurements) associated with the binding: 

Tibber Account:

| Channel ID         | Description                             | Read-only |
|--------------------|-----------------------------------------|-----------|
| Current Total      | Current Total Price (energy + tax)      | True      |
| Starts At          | Current Price Timestamp                 | True      |
| Daily Cost         | Daily Cost (last/previous day)          | True      |
| Daily Consumption  | Daily Consumption (last/previous day)   | True      |
| Daily From         | Timestamp (daily from)                  | True      |
| Daily To           | Timestamp (daily to)                    | True      |
| Hourly Cost        | Hourly Cost (last/previous hour)        | True      |
| Hourly Consumption | Hourly Consumption (last/previous hour) | True      |
| Hourly From        | Timestamp (hourly from)                 | True      |
| Hourly To          | Timestamp (hourly to)                   | True      |

Tibber Pulse:

| Channel ID              | Description                              | Read-only |
|-------------------------|------------------------------------------|-----------|
| Timestamp               | Timestamp for live measurements          | True      |
| Power                   | Live Power Consumption                   | True      |
| Last Meter Consumption  | Last Recorded Meter Consumption          | True      |
| Accumulated Consumption | Accumulated Consumption since Midnight   | True      |
| Accumulated Cost        | Accumulated Cost since Midnight          | True      |
| Currency                | Currency of Cost                         | True      |
| Min Power               | Min Power Consumption since Midnight     | True      |
| Average Power           | Average Power Consumption since Midnight | True      |
| Max Power               | Max Power Consumption since Midnight     | True      |
| Voltage 1-3             | Voltage per Phase                        | True      |
| Current 1-3             | Current per Phase                        | True      |
| Power Production        | Live Power Production                    | True      |
| Accumulated Production  | Accumulated Production since Midnight    | True      |
| Min Power Production    | Min Power Production since Midnight      | True      |
| Max Power Production    | Max Power Production since Midnight      | True      |


## Binding Configuration

To access and initiate the Tibber binding, a Tibber account is required.

Required input needed for initialization:

* Tibber token
* Tibber HomeId
* Refresh Interval (min 1 minute)

Note: Tibber token is retrieved from your Tibber account:
[Tibber Account](https://developer.tibber.com/settings/accesstoken)

Note: Tibber HomeId is retrieved from [www.developer.com](https://developer.tibber.com/explorer). Sign in (Tibber account) and load personal token. HomeId is retrieved by copying and running query below from Tibber API explorer. If Pulse is connected, realTimeConsumptionEnabled will report "true" for the associated HomeId. Copy desired HomeId, without quotation marks, and paste into PaperUI configuration.

```
{
  viewer {
    homes {
      id
      features {
        realTimeConsumptionEnabled
      }
    }
  }
}
```

If user have multiple HomeIds / Pulse, separate Things have to be created for the desired HomeIds in PaperUI (created manually).


## Thing Configuration

When Tibber binding is installed, Tibber API should be autodiscovered in PaperUI. Retrieve personal token and HomeId from description above, and initialize/start binding from PaperUI. Tibber API will be autodiscovered if provided input is correct.

Note: 
Gson is required. If not able to initialize binding, perform from OpenHab console:

```
bundle:install http://central.maven.org/maven2/com/google/code/gson/gson/2.8.5/gson-2.8.5.jar
```


## Full Example

demo.items:

```
Number:Dimensionless       TibberAPICurrentTotal                 "Current total price"            {channel="tibber:tibberapi:ae46b828:current_total"}
String                     TibberAPICurrentStartsAt              "Timestamp"                      {channel="tibber:tibberapi:ae46b828:current_startsAt"}
String                     TibberAPIDailyFrom                    "Timestamp"                      {channel="tibber:tibberapi:ae46b828:daily_from"}
String                     TibberAPIDailyTo                      "Timestamp"                      {channel="tibber:tibberapi:ae46b828:daily_to"}
Number:Dimensionless       TibberAPIDailyCost                    "Total cost"                     {channel="tibber:tibberapi:ae46b828:daily_cost"}
Number:Energy              TibberAPIDailyConsumption             "Total consumption"              {channel="tibber:tibberapi:ae46b828:daily_consumption"}
String                     TibberAPIHourlyFrom                   "Timestamp"                      {channel="tibber:tibberapi:ae46b828:hourly_from"}
String                     TibberAPIHourlyTo                     "Timestamp"                      {channel="tibber:tibberapi:ae46b828:hourly_to"}
Number:Dimensionless       TibberAPIHourlyCost                   "Total cost"                     {channel="tibber:tibberapi:ae46b828:hourly_cost"}
Number:Energy              TibberAPIHourlyConsumption            "Total consumption"              {channel="tibber:tibberapi:ae46b828:hourly_consumption"}
String                     TibberAPILiveTimestamp                "Timestamp"                      {channel="tibber:tibberapi:ae46b828:live_timestamp"}
Number:Power               TibberAPILivePower                    "Power consumption/production"   {channel="tibber:tibberapi:ae46b828:live_power"}
Number:Energy              TibberAPILiveLastMeterConsumption     "Total consumption"              {channel="tibber:tibberapi:ae46b828:live_lastMeterConsumption"}
Number:Energy              TibberAPILiveAccumulatedConsumption   "Total consumption"              {channel="tibber:tibberapi:ae46b828:live_accumulatedConsumption"}
Number:Dimensionless       TibberAPILiveAccumulatedCost          "Total cost"                     {channel="tibber:tibberapi:ae46b828:live_accumulatedCost"}
String                     TibberAPILiveCurrency                 "Currency"                       {channel="tibber:tibberapi:ae46b828:live_currency"}
Number:Power               TibberAPILiveMinPower                 "Power consumption/production"   {channel="tibber:tibberapi:ae46b828:live_minPower"}
Number:Power               TibberAPILiveAveragePower             "Power consumption/production"   {channel="tibber:tibberapi:ae46b828:live_averagePower"}
Number:Power               TibberAPILiveMaxPower                 "Power consumption/production"   {channel="tibber:tibberapi:ae46b828:live_maxPower"}
Number:ElectricPotential   TibberAPILiveVoltage1                 "Voltage"                        {channel="tibber:tibberapi:ae46b828:live_voltage1"}
Number:ElectricPotential   TibberAPILiveVoltage2                 "Voltage"                        {channel="tibber:tibberapi:ae46b828:live_voltage2"}
Number:ElectricPotential   TibberAPILiveVoltage3                 "Voltage"                        {channel="tibber:tibberapi:ae46b828:live_voltage3"}
Number:ElectricCurrent     TibberAPILiveCurrent1                 "Current"                        {channel="tibber:tibberapi:ae46b828:live_current1"}
Number:ElectricCurrent     TibberAPILiveCurrent2                 "Current"                        {channel="tibber:tibberapi:ae46b828:live_current2"}
Number:ElectricCurrent     TibberAPILiveCurrent3                 "Current"                        {channel="tibber:tibberapi:ae46b828:live_current3"}
Number:Power               TibberAPILivePowerProduction          "Power consumption/production"   {channel="tibber:tibberapi:ae46b828:live_powerProduction"}
Number:Power               TibberAPILiveMinPowerproduction       "Power consumption/production"   {channel="tibber:tibberapi:ae46b828:live_minPowerproduction"}
Number:Power               TibberAPILiveMaxPowerproduction       "Power consumption/production"   {channel="tibber:tibberapi:ae46b828:live_maxPowerproduction"}
Number:Energy              TibberAPILiveAccumulatedProduction    "Total production"               {channel="tibber:tibberapi:ae46b828:live_accumulatedProduction"}
```
