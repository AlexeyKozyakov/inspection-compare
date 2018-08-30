# Inspection Compare Plugin
Plugin for Intellij IDEA. Compare the inspection results you have exported to a directoryes in XML. 
This plugin adds a new "Filter/Diff Inspection Results..." button to the Analyze menu.

General usage instructions:

On the main menu, choose Analyze | Filter/Diff Inspection Results...
In the Filter/Diff dialog select the Diff tab if you want to compare inspection results or select the Filter tab if you want to filter the results by substring.

On the Diff tab, choose baseline and updated directores that contains results in XML format. Optionally, filter/normalize results before compare. Specify the output directoryes to store added and removed warnings in. Click Diff.

On the Filter tab, choose directory that also contains results in XML format and specify the filter substring. Specify output directory to store the filtering results in. Click Filter.
