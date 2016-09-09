# ST-leakSmart-Sensor

## Summary
This SmartThings device handler is meant for the leakSmart Sensor.

## Installation via GitHub Integration
1. Open SmartThings IDE in your web browser and log into your account.
2. Click on the "My Device Handlers" section in the navigation bar.
3. Click on "Settings".
4. Click "Add New Repository".
5. Enter "ericvitale" as the namespace.
6. Enter "ST-leakSmart-Sensor" as the repository.
7. Hit "Save".
8. Select "Update from Repo" and select "ST-leakSmart-Sensor".
9. Select "leaksmart-sensor.groovy".
10. Check "Publish" and hit "Execute".

## Manual Installation (if that is your thing)
1. Open SmartThings IDE in your web browser and log into your account.
2. Click on the "My Device Types" section in the navigation bar.
3. On your Device Types page, click on the "+ New Device Type" button on the right.
4 . On the "New Device Type" page, Select the Tab "From Code" , Copy the "leaksmart-sensor.groovy" source code from GitHub and paste it into the IDE editor window.
5. Click the blue "Create" button at the bottom of the page. An IDE editor window containing device handler template should now open.
6. Click the blue "Save" button above the editor window.
7. Click the "Publish" button next to it and select "For Me". You have now self-published your Device Handler.

## Pairing the Device
This device is a bit interesting to get it to pair. I've seen a few scenarios happen. Here is what I generally do:
1. Get your ST app into pairing mode.
2. Open the battery cover and remove one of the batteries.
3. Hold down the button insert the battery. Immediatly unpress the button and you should get a long beep (1 second).
4. Wait a second and then long press the button 2-3 seconds, after that it should start beeping and be discovered by ST. If it is not discovered by ST, try again. If still not discovered, press the button a few times quickly. 
5. ST should find it quickly once in pairing mode. If after you setup the device on ST it is still beeping, something went wrong. Delete the device from ST and reset it (hold the button for 5+ seconds, sometimes you have to do this more than once) and start over.
 
## Compatibility Mode
There is an optional setting in the device configuration. If you are having issues getting it to report wet/dry, try enabling compatibility mode.


## Acknowledgements
This code is by no means 100% all my work. This device handler is the work of @dhelm2, @John_Luikart, & @krlaframboise.
