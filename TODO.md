# TODO Liste - In a Nutshell

## **Problèmes actuels à corriger**

### Haute priorité
- [x] **Migration base de données** - Nouveau champ `portions` ajouté à MealPlan
- [x] **Dialog de sélection de recette avec quantité** - TERMINÉ - Interface ajoutée avec boutons +/-
- [x] **Affichage des portions dans le planificateur** - EN COURS - Les quantités ne s'affichent pas dans la vue hebdomadaire
- [ ] **Test complet de la sélection de quantité** - Vérifier que tout fonctionne end-to-end

### Faible priorité
- [ ] **Performance de l'application** - Optimiser les requêtes de base de données
- [ ] **Gestion d'erreurs** - Améliorer la robustesse générale
- [ ] **Tests** - Vérifier toutes les fonctionnalités après modifications
- [ ] **Code cleanup** - Supprimer les warnings de compilation

---

## **Nouvelles fonctionnalités à implémenter**

### Phase 1 - Améliorations rapides

#### **Mode sombre**
- [ ] Ajouter un thème sombre automatique
- [ ] Détection des préférences système
- [ ] Bouton de basculement manuel dans les réglages
- [ ] Adapter tous les layouts et couleurs

#### **Recherche avancée**
- [ ] Filtres par temps de préparation (< 30min, 30-60min, > 1h)
- [ ] Recherche par ingrédients disponibles
- [ ] Filtre par nombre de portions
- [ ] Recherche par tags/catégories
- [ ] Sauvegarde des recherches favorites

#### **Interface utilisateur**
- [ ] Améliorer l'animation des transitions
- [ ] Ajouter des haptic feedback
- [ ] Optimiser pour les tablettes
- [ ] Support des gestes (swipe pour supprimer, etc.)
- [ ] Raccourcis clavier pour les émulateurs

#### **Système de tags/catégories**
- [ ] Créer une entité Tag en base
- [ ] Interface de gestion des tags
- [ ] Association tags-recettes (relation many-to-many)
- [ ] Filtrage par tags
- [ ] Tags prédéfinis (végétarien, sans gluten, etc.)

### Phase 2 - Fonctionnalités moyennes

#### **Liste de courses automatique**
- [ ] Génération automatique depuis le planificateur
- [ ] Regroupement par type d'ingrédient
- [ ] Calcul des quantités totales
- [ ] Interface de modification manuelle
- [ ] Cochage des articles achetés
- [ ] Sauvegarde des listes

#### **Statistiques et analyses**
- [ ] Recettes les plus cuisinées
- [ ] Temps de préparation moyens
- [ ] Graphiques d'utilisation
- [ ] Recommandations basées sur l'historique
- [ ] Export des données

#### **Synchronisation et sauvegarde**
- [ ] Export/Import des recettes (JSON, CSV)
- [ ] Sauvegarde automatique dans le cloud
- [ ] Synchronisation multi-appareils
- [ ] Partage de recettes entre utilisateurs

#### **Améliorations du timer**
- [ ] Timers multiples simultanés
- [ ] Timers nommés et personnalisés
- [ ] Notifications push même app fermée
- [ ] Sons de notification personnalisables
- [ ] Historique des timers utilisés

### Phase 3 - Fonctionnalités avancées

#### **Planification avancée**
- [ ] Planification sur plusieurs semaines
- [ ] Modèles de planning récurrents
- [ ] Optimisation nutritionnelle
- [ ] Gestion des régimes alimentaires
- [ ] Calcul automatique des coûts

#### **Outils avancés**
- [ ] Calculateur de substitutions d'ingrédients
- [ ] Guide des saisons pour fruits/légumes
- [ ] Base de données nutritionnelle

---

## **Améliorations techniques**

### Architecture et performance
- [ ] **Migration vers Room Database version plus récente**
- [ ] **Implémentation du pattern MVVM**
- [ ] **Ajout de tests unitaires et d'intégration**
- [ ] **Optimisation des requêtes SQL**
- [ ] **Mise en cache des images**
- [ ] **Lazy loading pour les listes**

### Sécurité et robustesse
- [ ] **Chiffrement des données sensibles**
- [ ] **Validation des entrées utilisateur**
- [ ] **Gestion des erreurs réseau**
- [ ] **Récupération automatique après crash**
- [ ] **Logs structurés pour le debugging**

### Code quality
- [ ] **Refactoring des classes trop grandes**
- [ ] **Documentation du code**
- [ ] **Standardisation du style de code**
- [ ] **Suppression du code mort**
- [ ] **Optimisation des imports**

---

## **Design et UX**

### Interface
- [ ] **Redesign des icônes**
- [ ] **Animations fluides**
- [ ] **Meilleure hiérarchie visuelle**
- [ ] **Feedback visuel pour les actions**
- [ ] **Écrans d'onboarding**

### Accessibilité
- [ ] **Support des lecteurs d'écran**
- [ ] **Tailles de police ajustables**
- [ ] **Contraste élevé**
- [ ] **Navigation clavier**
- [ ] **Support des langues multiples**

---

## **Documentation**

- [ ] **Guide utilisateur complet**
- [ ] **Documentation développeur**
- [ ] **Architecture de l'application**
- [ ] **API documentation**
- [ ] **Guide de contribution**
- [ ] **Changelog détaillé**

---

## **Objectifs à long terme**

### Version 2.0
- [ ] Refonte complète de l'interface
- [ ] Synchronisation cloud

### Version 3.0
- [ ] Application web compagnon
- [ ] API publique
- [ ] Marketplace de recettes

---

## **Métriques de succès**

- [ ] **Performance** : Temps de démarrage < 2s
- [ ] **Stabilité** : Taux de crash < 0.1%
- [ ] **Engagement** : Temps moyen d'utilisation > 5min
- [ ] **Satisfaction** : Note Play Store > 4.5/5
- [ ] **Adoption** : 1000+ utilisateurs actifs

---

*Dernière mise à jour : 23 juillet 2025*
*Statut actuel : Développement actif - Finalisation de la sélection de quantité, prêt pour les tests utilisateur*

---

## **Liens utiles**

- [Documentation Android](https://developer.android.com/docs)
- [Material Design Guidelines](https://material.io/design)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Best Practices Android](https://developer.android.com/guide/navigation/navigation-principles)
