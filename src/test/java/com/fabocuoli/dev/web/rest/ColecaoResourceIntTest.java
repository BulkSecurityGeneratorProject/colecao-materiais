package com.fabocuoli.dev.web.rest;

import com.fabocuoli.dev.ColecaoDeMateriaisApp;

import com.fabocuoli.dev.domain.Colecao;
import com.fabocuoli.dev.repository.ColecaoRepository;
import com.fabocuoli.dev.service.ColecaoService;
import com.fabocuoli.dev.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;


import static com.fabocuoli.dev.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the ColecaoResource REST controller.
 *
 * @see ColecaoResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ColecaoDeMateriaisApp.class)
public class ColecaoResourceIntTest {

    private static final String DEFAULT_NOME = "AAAAAAAAAA";
    private static final String UPDATED_NOME = "BBBBBBBBBB";

    @Autowired
    private ColecaoRepository colecaoRepository;

    @Autowired
    private ColecaoService colecaoService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restColecaoMockMvc;

    private Colecao colecao;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final ColecaoResource colecaoResource = new ColecaoResource(colecaoService);
        this.restColecaoMockMvc = MockMvcBuilders.standaloneSetup(colecaoResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Colecao createEntity(EntityManager em) {
        Colecao colecao = new Colecao()
            .nome(DEFAULT_NOME);
        return colecao;
    }

    @Before
    public void initTest() {
        colecao = createEntity(em);
    }

    @Test
    @Transactional
    public void createColecao() throws Exception {
        int databaseSizeBeforeCreate = colecaoRepository.findAll().size();

        // Create the Colecao
        restColecaoMockMvc.perform(post("/api/colecaos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(colecao)))
            .andExpect(status().isCreated());

        // Validate the Colecao in the database
        List<Colecao> colecaoList = colecaoRepository.findAll();
        assertThat(colecaoList).hasSize(databaseSizeBeforeCreate + 1);
        Colecao testColecao = colecaoList.get(colecaoList.size() - 1);
        assertThat(testColecao.getNome()).isEqualTo(DEFAULT_NOME);
    }

    @Test
    @Transactional
    public void createColecaoWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = colecaoRepository.findAll().size();

        // Create the Colecao with an existing ID
        colecao.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restColecaoMockMvc.perform(post("/api/colecaos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(colecao)))
            .andExpect(status().isBadRequest());

        // Validate the Colecao in the database
        List<Colecao> colecaoList = colecaoRepository.findAll();
        assertThat(colecaoList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllColecaos() throws Exception {
        // Initialize the database
        colecaoRepository.saveAndFlush(colecao);

        // Get all the colecaoList
        restColecaoMockMvc.perform(get("/api/colecaos?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(colecao.getId().intValue())))
            .andExpect(jsonPath("$.[*].nome").value(hasItem(DEFAULT_NOME.toString())));
    }
    
    @Test
    @Transactional
    public void getColecao() throws Exception {
        // Initialize the database
        colecaoRepository.saveAndFlush(colecao);

        // Get the colecao
        restColecaoMockMvc.perform(get("/api/colecaos/{id}", colecao.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(colecao.getId().intValue()))
            .andExpect(jsonPath("$.nome").value(DEFAULT_NOME.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingColecao() throws Exception {
        // Get the colecao
        restColecaoMockMvc.perform(get("/api/colecaos/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateColecao() throws Exception {
        // Initialize the database
        colecaoService.save(colecao);

        int databaseSizeBeforeUpdate = colecaoRepository.findAll().size();

        // Update the colecao
        Colecao updatedColecao = colecaoRepository.findById(colecao.getId()).get();
        // Disconnect from session so that the updates on updatedColecao are not directly saved in db
        em.detach(updatedColecao);
        updatedColecao
            .nome(UPDATED_NOME);

        restColecaoMockMvc.perform(put("/api/colecaos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedColecao)))
            .andExpect(status().isOk());

        // Validate the Colecao in the database
        List<Colecao> colecaoList = colecaoRepository.findAll();
        assertThat(colecaoList).hasSize(databaseSizeBeforeUpdate);
        Colecao testColecao = colecaoList.get(colecaoList.size() - 1);
        assertThat(testColecao.getNome()).isEqualTo(UPDATED_NOME);
    }

    @Test
    @Transactional
    public void updateNonExistingColecao() throws Exception {
        int databaseSizeBeforeUpdate = colecaoRepository.findAll().size();

        // Create the Colecao

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restColecaoMockMvc.perform(put("/api/colecaos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(colecao)))
            .andExpect(status().isBadRequest());

        // Validate the Colecao in the database
        List<Colecao> colecaoList = colecaoRepository.findAll();
        assertThat(colecaoList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteColecao() throws Exception {
        // Initialize the database
        colecaoService.save(colecao);

        int databaseSizeBeforeDelete = colecaoRepository.findAll().size();

        // Delete the colecao
        restColecaoMockMvc.perform(delete("/api/colecaos/{id}", colecao.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Colecao> colecaoList = colecaoRepository.findAll();
        assertThat(colecaoList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Colecao.class);
        Colecao colecao1 = new Colecao();
        colecao1.setId(1L);
        Colecao colecao2 = new Colecao();
        colecao2.setId(colecao1.getId());
        assertThat(colecao1).isEqualTo(colecao2);
        colecao2.setId(2L);
        assertThat(colecao1).isNotEqualTo(colecao2);
        colecao1.setId(null);
        assertThat(colecao1).isNotEqualTo(colecao2);
    }
}
