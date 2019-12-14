How to install
---
locate the apk from the folder app/release/app-release.apk 
copy it into your phone and click it to install. 


How to send bulk sms.
---
Open the MessejiNyingi app in your Phone, make sure your phone and your computer are in the same wireless network or hotspot, 
then open your computer browser and in its address bar type the url shown in your phone.
In the page which opens up, click *choosefile* button and then locate an *Excel file* which contains the phone numbers and sms you want to send. 
Hit *send*, then observe in your browser and in phone as the sms are being sent. To cancel sending, hit *Cancel* from your browser.


>#### NOTE
> Once the sms is in your phone, that sms cannot be stopped from being sent, it is as goog as sent already, the only point of interrupting  sending is in the browser before it is sent to your phone.


What to have in Excel file
----
Only two columns are mandatory, the *phone* column and the *sms* column, every other column will be ignored unless it is referred to from within the text in the sms column.

In your sms column you can refer to values from other column by  `${column_name}`, this will substitute the `${column_name}` by the value of  the column_name at that row


for example

phone|sms|jina
----|----|--
25510283784783|Hello ${jina}, your phone number ${phone} has been successfully saved|John Doe

this will result into a message: `Hello Jon Doe, Your phone number  25510283784783 has been successfully saved`;


