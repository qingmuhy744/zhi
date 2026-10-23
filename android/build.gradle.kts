plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}

// AGP 9.4.1 still requests these on its build classpath. Substitute the
// patched coordinates so the dependency graph does not keep the old jars.
// commons-compress 1.27.1 requests commons-lang3 3.16.0 (GHSA-j288-q9x7-2f5v).
// httpmime 4.5.6 still resolves httpclient 4.5.6 (GHSA-7r82-7xv7-xcpj).
buildscript {
    configurations.classpath {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.bouncycastle:bcprov-jdk18on")).using(module("org.bouncycastle:bcprov-jdk18on:1.85"))
            substitute(module("org.bouncycastle:bcpkix-jdk18on")).using(module("org.bouncycastle:bcpkix-jdk18on:1.85"))
            substitute(module("org.bouncycastle:bcutil-jdk18on")).using(module("org.bouncycastle:bcutil-jdk18on:1.85"))
            substitute(module("org.bitbucket.b_c:jose4j")).using(module("org.bitbucket.b_c:jose4j:0.9.6"))
            substitute(module("org.jdom:jdom2")).using(module("org.jdom:jdom2:2.0.6.1"))
            substitute(module("org.apache.commons:commons-lang3")).using(module("org.apache.commons:commons-lang3:3.18.0"))
            substitute(module("org.apache.httpcomponents:httpclient")).using(module("org.apache.httpcomponents:httpclient:4.5.14"))
        }
    }
    dependencies {
        classpath("org.bouncycastle:bcprov-jdk18on:1.85")
        classpath("org.bouncycastle:bcpkix-jdk18on:1.85")
        classpath("org.bouncycastle:bcutil-jdk18on:1.85")
        classpath("org.bitbucket.b_c:jose4j:0.9.6")
        classpath("org.jdom:jdom2:2.0.6.1")
        classpath("org.apache.commons:commons-lang3:3.18.0")
        classpath("org.apache.httpcomponents:httpclient:4.5.14")
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
            substitute(module("org.apache.commons:commons-lang3")).using(module("org.apache.commons:commons-lang3:3.18.0"))
            substitute(module("org.apache.httpcomponents:httpclient")).using(module("org.apache.httpcomponents:httpclient:4.5.14"))
        }
    }
}
