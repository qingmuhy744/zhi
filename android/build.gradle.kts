plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}

// AGP 9.4.1 still requests these on its build classpath. Substitute the
// patched coordinates so the dependency graph does not keep the old jars.
buildscript {
    configurations.classpath {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.bouncycastle:bcprov-jdk18on")).using(module("org.bouncycastle:bcprov-jdk18on:1.85"))
            substitute(module("org.bouncycastle:bcpkix-jdk18on")).using(module("org.bouncycastle:bcpkix-jdk18on:1.85"))
            substitute(module("org.bouncycastle:bcutil-jdk18on")).using(module("org.bouncycastle:bcutil-jdk18on:1.85"))
            substitute(module("org.bitbucket.b_c:jose4j")).using(module("org.bitbucket.b_c:jose4j:0.9.6"))
            substitute(module("org.jdom:jdom2")).using(module("org.jdom:jdom2:2.0.6.1"))
        }
    }
    dependencies {
        classpath("org.bouncycastle:bcprov-jdk18on:1.85")
        classpath("org.bouncycastle:bcpkix-jdk18on:1.85")
        classpath("org.bouncycastle:bcutil-jdk18on:1.85")
        classpath("org.bitbucket.b_c:jose4j:0.9.6")
        classpath("org.jdom:jdom2:2.0.6.1")
    }
}

subprojects {
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.bouncycastle:bcprov-jdk18on")).using(module("org.bouncycastle:bcprov-jdk18on:1.85"))
            substitute(module("org.bouncycastle:bcpkix-jdk18on")).using(module("org.bouncycastle:bcpkix-jdk18on:1.85"))
            substitute(module("org.bouncycastle:bcutil-jdk18on")).using(module("org.bouncycastle:bcutil-jdk18on:1.85"))
            // firebase-iid-interop and firebase-measurement-connector still request 18.0.0.
            substitute(module("com.google.android.gms:play-services-basement")).using(module("com.google.android.gms:play-services-basement:18.3.0"))
        }
    }
}
