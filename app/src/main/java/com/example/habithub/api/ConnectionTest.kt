package com.example.habithub.api

import java.net.HttpURLConnection
import java.net.URL

object ConnectionTest {

    fun test(
        onResult: (String) -> Unit
    ) {

        Thread {

            try {

                val url =
                    URL("http://10.0.2.2:5076/api/Test")

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"

                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                connection.connect()

                val responseCode =
                    connection.responseCode

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                connection.disconnect()

                onResult(
                    "HTTP TEST SUCCESS\n" +
                            "Code: $responseCode\n" +
                            response
                )

            } catch (e: Exception) {

                onResult(
                    "HTTP TEST FAILED\n" +
                            "${e.javaClass.simpleName}\n" +
                            "${e.message}"
                )
            }

        }.start()
    }
}