package br.com.igorbag.githubsearch.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var nomeUsuario: EditText
    private lateinit var btnConfirmar: Button
    private lateinit var listaRepositories: RecyclerView
    private lateinit var githubApi: GitHubService
    private lateinit var repositoryAdapter: RepositoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        setupListeners()
        showUserName()
        setupRetrofit()
    }

    // Metodo responsavel por realizar o setup da view e recuperar os Ids do layout
    private fun setupView() {
        nomeUsuario = findViewById(R.id.et_nome_usuario)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        listaRepositories = findViewById(R.id.rv_lista_repositories)
    }

    //metodo responsavel por configurar os listeners click da tela
    private fun setupListeners() {
        btnConfirmar.setOnClickListener {
            Toast.makeText(
                this, "Procurando repositórios de ${nomeUsuario.text}", Toast.LENGTH_LONG
            ).show()
            saveUserLocal(nomeUsuario.text.toString())
            getAllReposByUserName()
        }
    }


    // salvar o usuario preenchido no EditText utilizando uma SharedPreferences
    private fun saveUserLocal(userName: String) {
        val sharedPref = this.getPreferences(MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(getString(R.string.saved_user_name), userName)
            apply()
        }
    }

    private fun showUserName() {
        val sharedPref = this.getPreferences(MODE_PRIVATE)
        val userName = sharedPref.getString(getString(R.string.saved_user_name), "")

        if (!userName.isNullOrEmpty() || userName != "") {
            nomeUsuario.setText(userName)
        }
    }

    //Metodo responsavel por fazer a configuracao base do Retrofit
    private fun setupRetrofit() {
        val retrofit =
            Retrofit.Builder().baseUrl("https://api.github.com/").addConverterFactory(
                GsonConverterFactory.create())
                .build()

        githubApi = retrofit.create(GitHubService::class.java)
    }

    //Metodo responsavel por buscar todos os repositorios do usuario fornecido
    private fun getAllReposByUserName() {
        val sharedPref = this.getPreferences(MODE_PRIVATE)
        val userName = sharedPref.getString(getString(R.string.saved_user_name), "")

        try {
            val call = githubApi.getAllRepositoriesByUser(userName!!)
            call.enqueue(object : Callback<List<Repository>> {
                override fun onResponse(
                    call: Call<List<Repository>>, response: Response<List<Repository>>
                ) {
                    if (response.isSuccessful) {
                        val repositories = response.body()
                        setupAdapter(repositories!!)
                    }
                }

                override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                    Toast.makeText(
                        this@MainActivity, "Erro ao buscar os repositorios", Toast.LENGTH_LONG
                    ).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(
                this@MainActivity, "Erro ao buscar os repositorios", Toast.LENGTH_LONG
            ).show()
        } finally {
            Toast.makeText(
                this@MainActivity, "Busca finalizada", Toast.LENGTH_LONG
            ).show()
        }
    }

    // Metodo responsavel por realizar a configuracao do adapter
    fun setupAdapter(list: List<Repository>) {
        repositoryAdapter = RepositoryAdapter(list)
        listaRepositories.adapter = repositoryAdapter

        repositoryAdapter.repositoryItemLister = {
            openBrowser(it.htmlUrl)
        }

        repositoryAdapter.btnShareLister = {
            shareRepositoryLink(it.htmlUrl)
        }
    }


    // Metodo responsavel por compartilhar o link do repositorio selecionado
    private fun shareRepositoryLink(urlRepository: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, urlRepository)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    // Metodo responsavel por abrir o browser com o link informado do repositorio

    private fun openBrowser(urlRepository: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(urlRepository)
            )
        )

    }

}