#!/usr/bin/env ruby
# Clear two App Store Connect submission gates for SLOVO, idempotently:
#
#   1. Age rating -> 4+ (all content NONE, all booleans false), via
#      AppInfo -> AgeRatingDeclaration (ASC API 1.3+; it moved off AppStoreVersion).
#   2. Content rights -> USES_THIRD_PARTY_CONTENT. SLOVO bundles Tatoeba CC-BY
#      audio, which IS third-party content, licensed with attribution shown in
#      the app under YOU -> About -> View Full Credits. Declared truthfully.
#
# Env: ASC_KEY_ID, ASC_ISSUER_ID, ASC_APP_ID (see fastlane/.env.example).
require "spaceship"

APP = ENV.fetch("ASC_APP_ID") { abort("Set ASC_APP_ID (numeric Apple App ID)") }
P8  = Dir[File.expand_path("../AuthKey_*.p8", __dir__)].first
abort("No AuthKey_*.p8 in the repo root") unless P8

Spaceship::ConnectAPI.token = Spaceship::ConnectAPI::Token.create(
  key_id: ENV.fetch("ASC_KEY_ID", "948K3FKL2H"),
  issuer_id: ENV.fetch("ASC_ISSUER_ID"),
  filepath: File.expand_path(P8)
)

app = Spaceship::ConnectAPI::App.get(app_id: APP)
info = app.fetch_edit_app_info
puts "Editable AppInfo: #{info.id}"

ard = info.fetch_age_rating_declaration
puts "AgeRatingDeclaration: #{ard.id}"

# 4+ : no objectionable content of any kind, no interactive/exposure features.
attributes = {
  "alcoholTobaccoOrDrugUseOrReferences" => "NONE",
  "contests" => "NONE",
  "gamblingSimulated" => "NONE",
  "gunsOrOtherWeapons" => "NONE",
  "horrorOrFearThemes" => "NONE",
  "matureOrSuggestiveThemes" => "NONE",
  "medicalOrTreatmentInformation" => "NONE",
  "profanityOrCrudeHumor" => "NONE",
  "sexualContentGraphicAndNudity" => "NONE",
  "sexualContentOrNudity" => "NONE",
  "violenceCartoonOrFantasy" => "NONE",
  "violenceRealisticProlongedGraphicOrSadistic" => "NONE",
  "violenceRealistic" => "NONE",
  "advertising" => false,
  "ageAssurance" => false,
  "gambling" => false,
  "healthOrWellnessTopics" => false,
  "lootBox" => false,
  "messagingAndChat" => false,
  "parentalControls" => false,
  "unrestrictedWebAccess" => false,
  "userGeneratedContent" => false,
  "ageRatingOverrideV2" => "NONE",
  "koreaAgeRatingOverride" => "NONE",
  "kidsAgeBand" => nil
}

ard.update(attributes: attributes)
puts "Updated age rating attributes."

info2 = app.fetch_edit_app_info
puts "App Store age rating now: #{info2.app_store_age_rating.inspect}"

# Content rights: SLOVO ships licensed third-party audio (Tatoeba, CC-BY).
Spaceship::ConnectAPI.patch(
  url_or_path: "apps/#{APP}",
  body: {
    data: {
      type: "apps",
      id: APP,
      attributes: { contentRightsDeclaration: "USES_THIRD_PARTY_CONTENT" }
    }
  }
)
puts "Content rights set to USES_THIRD_PARTY_CONTENT."
