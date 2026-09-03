#!/bin/bash

# add to build.gradle.kts
sed -i 's/testImplementation(libs.junit)/testImplementation(libs.junit)\n    testImplementation("org.robolectric:robolectric:4.11.1")\n    testImplementation("androidx.test:core:1.5.0")\n    testImplementation("androidx.test.ext:junit:1.1.5")/g' app/build.gradle.kts
