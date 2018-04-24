# Chapter 4 - You are more than just a suit

Ouch...suite à votre combat du chapitre 3, votre super combinaison `chainsaw` vient de tomber. Vous devez maintenant configurer Gradle sans lui.

Supprimez le plugin chainsaw de l'ensemble des fichiers `build.gradle` et les hack junit associés.

```
git checkout j9-vertx-no-chainsaw
```

Ajoutez dans l'ensemble des ces fichiers une `ExtraPropertyExtension` qui aura le nom de votre module (voir exemple).

```
ext.moduleName = "my.module"
```

**build.gradle**

* Ajoutez un hook sur la tâche `compileJava` pour ajouter dans l'argument `--module-path` les valeurs retournées par `classpath.asPath`.
* Ajoutez un hook sur la tâche `compileTestJava` pour configurer `javac` avec les arguments `--module-path`, `--add-module`, `--add-reads`, `--patch-module`.
* Ajoutez un hook sur la tâche `test` pour configurer `javac` avec les arguments `--module-path`, `--add-module`, `--add-reads`, `--patch-module`.

> `classpath.asPath` retourne le classpath tel que calculé en fonction des dépendences définies dans le projet gradle.

Encore une fois n'hésitez à vous appuyer sur vos [TIPS](./TIPS.md) en cas de problème(s)!

**marvel-http-server/build.gradle**

Ajoutez un hook sur la tâche `run` pour ajouter les arguments jvm `--modulePath` et `--module`.
Ajoutez un hook sur la tâche `startScripts` avec le contenu suivant.

```
import java.util.regex.Matcher // Matcher is use for replacing args in batchs files
...

startScripts {
    inputs.property("moduleName", moduleName)
    doFirst {
        classpath = files()
        defaultJvmOpts = [
                '--module-path', 'APP_HOME_LIBS', // Multi plateform replace in doLast
                '-Dport=8080', '-Ddev=false',
                '--module', mainClassName
        ]
    }
    doLast{
        def bashFile = new File(outputDir, applicationName)
        String bashContent = bashFile.text
        bashFile.text = bashContent.replaceFirst('APP_HOME_LIBS', Matcher.quoteReplacement('$APP_HOME/lib'))

        def batFile = new File(outputDir, applicationName + ".bat")
        String batContent = batFile.text
        batFile.text = batContent.replaceFirst('APP_HOME_LIBS', Matcher.quoteReplacement('%APP_HOME%\\lib'))
    }
}
```

Buildez et lancez votre serveur, qui doit fonctionner comme avant!

> Bravo vous avez réussi à configurer Gradle sans chainsaw et vous maitrisez maintenant pleinement vos pouvoirs! Si la force est avec vous, rendez-vous sur [l'exercice suivant](./CHAPTER_5.md).
