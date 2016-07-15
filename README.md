# ST-leakSmart-Sensor

## Summary
This SmartThings device handler is meant for the leakSmart Sensor.

## Installation via GitHub Integration
1. Open SmartThings IDE in your web browser and log into your account.
2. Click on the "My Device Types" section in the navigation bar.
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

## Post Installation Configuration
1. Navigate to the device on your SmartThings app.
2. Press the gear icons.
3. Press done (or update the log level and done).
4. Wait about 10 - 15 seconds.
5. Press the "refresh" button.
6. Wait about 10 - 15 seconds.
7. Press the "configure" button.
8. Wait about 10 - 15 seconds.
9. Temperature and battery level should begin to appear. 
10. If they don't begin to appear, do another cycle of 2-8.


## Acknowledgements
This code is by no means 100% all my work. This device handler is the work of SmartThings, @dhelm2, & @John_Luikart.
I've simply adjusted for format, added better loggging, and updated the code to not require the simulator to config. 
