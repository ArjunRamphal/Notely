sed -i 's/testImplementation(libs.junit)/testImplementation(libs.junit)\n    testImplementation("io.mockk:mockk:1.13.8")/g' app/build.gradle.kts
