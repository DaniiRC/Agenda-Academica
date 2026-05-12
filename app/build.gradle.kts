plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.agendaacademica"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.agendaacademica"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources.excludes.add("META-INF/INDEX.LIST")
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/io.netty.versions.properties")
        resources.excludes.add("META-INF/LICENSE*")
        resources.excludes.add("META-INF/NOTICE*")
    }

    project.tasks.register<Javadoc>("generateJavadoc") {
        description = "Genera Javadoc para el proyecto"
        group = "documentation"
        
        source = fileTree("src/main/java")
        
        doFirst {
            val debugVariant = applicationVariants.find { it.name == "debug" }
            if (debugVariant != null) {
                classpath = debugVariant.javaCompileProvider.get().classpath + 
                            files(bootClasspath.joinToString(File.pathSeparator))
            }
        }

        options {
            encoding = "UTF-8"
            (this as StandardJavadocDocletOptions).apply {
                charSet("UTF-8")
                locale = "es_ES"
                addStringOption("Xdoclint:none", "-quiet")
            }
        }

        setDestinationDir(file("${project.layout.buildDirectory.get()}/outputs/javadoc"))
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.airbnb.android:lottie:6.1.0")
    
    // Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    
    // Arquitectura Android (ViewModel y LiveData)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")
    
    // Glide para imágenes profesionales
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // Testing Dependencies
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    // Instrumented Testing
    androidTestImplementation("org.mockito:mockito-android:5.11.0")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
}
