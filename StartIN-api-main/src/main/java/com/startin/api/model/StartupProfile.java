package com.startin.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "startup_profiles")
public class StartupProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Etapa 1: Identidade
    @Column(nullable = false)
    private String name = "Nexa Health";

    private String mainArea = "Healthtech";

    @Column(length = 2000)
    private String description = "Plataforma de IA para apoiar a gestão e análise de dados em saúde.";

    private String location = "Recife, PE";
    private String cnpj = "12.345.678/0001-90";
    private String website = "https://nexahealth.com.br";
    private String foundedYear = "2024";

    // Etapa 2: Solução
    @Column(length = 2000)
    private String problem = "Gestão ineficiente e decisões baseadas em dados fragmentados.";

    @Column(length = 2000)
    private String howItWorks = "Plataforma integrada de agregação e insights em tempo real via IA.";

    @Column(length = 2000)
    private String differentiator = "Algoritmos preditivos customizados e compliance nativo com dados clínicos.";

    @Column(length = 2000)
    private String impact = "Redução de custos operacionais e otimização do atendimento ao paciente.";

    private String technologies = "IA, Dados, Automação, SaaS B2B";
    private String businessModel = "SaaS B2B";

    // Etapa 3: Mercado
    private String targetAudience = "Empresas de saúde e operações B2B";
    private String mainSector = "Saúde";
    private String operationRegion = "Brasil";
    private String state = "Pernambuco";
    private String customerProfile = "Médias e grandes empresas";
    private String marketSize = "R$ 2 bilhões";
    private String competitors = "Sistemas legados de gestão e prontuários isolados";
    private String mainChannel = "Vendas consultivas";

    // Etapa 4: Momento
    private String currentStage = "Tração";
    private String tractionCustomers = "12 clientes ativos";
    private String productMaturity = "Produto validado";
    private String teamSize = "8 pessoas";
    private String annualRevenue = "R$ 240 mil";
    private String recentGrowth = "+80% no último ano";
    private String investmentRaised = "R$ 500 mil";

    // Etapa 5: Objetivos
    private String mainObjective = "Expandir para novos mercados";
    private String investmentNeed = "R$ 1 milhão";
    private String executionDeadline = "12 meses";
    private String resourceUse = "Produto, equipe e expansão comercial";
    private String twelveMonthGoal = "Dobrar a base de clientes";
    private String strategicPriority = "Escala comercial";
    private String desiredPartners = "Fusões, canais e clientes";

    // Etapa 6: Preferências de Matching
    private String areasOfInterest = "IA, Saúde, Dados, Automação";
    private String opportunityTypes = "POC, Compra, Editais";
    private Boolean receiveAiRecommendations = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StartupProfile() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMainArea() {
        return mainArea;
    }

    public void setMainArea(String mainArea) {
        this.mainArea = mainArea;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getFoundedYear() {
        return foundedYear;
    }

    public void setFoundedYear(String foundedYear) {
        this.foundedYear = foundedYear;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public String getHowItWorks() {
        return howItWorks;
    }

    public void setHowItWorks(String howItWorks) {
        this.howItWorks = howItWorks;
    }

    public String getDifferentiator() {
        return differentiator;
    }

    public void setDifferentiator(String differentiator) {
        this.differentiator = differentiator;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getTechnologies() {
        return technologies;
    }

    public void setTechnologies(String technologies) {
        this.technologies = technologies;
    }

    public String getBusinessModel() {
        return businessModel;
    }

    public void setBusinessModel(String businessModel) {
        this.businessModel = businessModel;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getMainSector() {
        return mainSector;
    }

    public void setMainSector(String mainSector) {
        this.mainSector = mainSector;
    }

    public String getOperationRegion() {
        return operationRegion;
    }

    public void setOperationRegion(String operationRegion) {
        this.operationRegion = operationRegion;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCustomerProfile() {
        return customerProfile;
    }

    public void setCustomerProfile(String customerProfile) {
        this.customerProfile = customerProfile;
    }

    public String getMarketSize() {
        return marketSize;
    }

    public void setMarketSize(String marketSize) {
        this.marketSize = marketSize;
    }

    public String getCompetitors() {
        return competitors;
    }

    public void setCompetitors(String competitors) {
        this.competitors = competitors;
    }

    public String getMainChannel() {
        return mainChannel;
    }

    public void setMainChannel(String mainChannel) {
        this.mainChannel = mainChannel;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public String getTractionCustomers() {
        return tractionCustomers;
    }

    public void setTractionCustomers(String tractionCustomers) {
        this.tractionCustomers = tractionCustomers;
    }

    public String getProductMaturity() {
        return productMaturity;
    }

    public void setProductMaturity(String productMaturity) {
        this.productMaturity = productMaturity;
    }

    public String getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(String teamSize) {
        this.teamSize = teamSize;
    }

    public String getAnnualRevenue() {
        return annualRevenue;
    }

    public void setAnnualRevenue(String annualRevenue) {
        this.annualRevenue = annualRevenue;
    }

    public String getRecentGrowth() {
        return recentGrowth;
    }

    public void setRecentGrowth(String recentGrowth) {
        this.recentGrowth = recentGrowth;
    }

    public String getInvestmentRaised() {
        return investmentRaised;
    }

    public void setInvestmentRaised(String investmentRaised) {
        this.investmentRaised = investmentRaised;
    }

    public String getMainObjective() {
        return mainObjective;
    }

    public void setMainObjective(String mainObjective) {
        this.mainObjective = mainObjective;
    }

    public String getInvestmentNeed() {
        return investmentNeed;
    }

    public void setInvestmentNeed(String investmentNeed) {
        this.investmentNeed = investmentNeed;
    }

    public String getExecutionDeadline() {
        return executionDeadline;
    }

    public void setExecutionDeadline(String executionDeadline) {
        this.executionDeadline = executionDeadline;
    }

    public String getResourceUse() {
        return resourceUse;
    }

    public void setResourceUse(String resourceUse) {
        this.resourceUse = resourceUse;
    }

    public String getTwelveMonthGoal() {
        return twelveMonthGoal;
    }

    public void setTwelveMonthGoal(String twelveMonthGoal) {
        this.twelveMonthGoal = twelveMonthGoal;
    }

    public String getStrategicPriority() {
        return strategicPriority;
    }

    public void setStrategicPriority(String strategicPriority) {
        this.strategicPriority = strategicPriority;
    }

    public String getDesiredPartners() {
        return desiredPartners;
    }

    public void setDesiredPartners(String desiredPartners) {
        this.desiredPartners = desiredPartners;
    }

    public String getAreasOfInterest() {
        return areasOfInterest;
    }

    public void setAreasOfInterest(String areasOfInterest) {
        this.areasOfInterest = areasOfInterest;
    }

    public String getOpportunityTypes() {
        return opportunityTypes;
    }

    public void setOpportunityTypes(String opportunityTypes) {
        this.opportunityTypes = opportunityTypes;
    }

    public Boolean getReceiveAiRecommendations() {
        return receiveAiRecommendations;
    }

    public void setReceiveAiRecommendations(Boolean receiveAiRecommendations) {
        this.receiveAiRecommendations = receiveAiRecommendations;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
