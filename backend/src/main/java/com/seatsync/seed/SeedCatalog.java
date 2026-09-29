package com.seatsync.seed;

import com.seatsync.dto.admin.SectionPricing;
import com.seatsync.entity.EventCategory;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Static content for the development seed: fictional events at real-sounding Indian venues. */
final class SeedCatalog {

    private SeedCatalog() {
    }

    record EventTemplate(String name, EventCategory category, String description) {
    }

    record Venue(String name, String city) {
    }

    static final List<Venue> VENUES = List.of(
            new Venue("SRM Auditorium", "Chennai"),
            new Venue("Music Academy", "Chennai"),
            new Venue("Chowdiah Memorial Hall", "Bengaluru"),
            new Venue("Bangalore International Centre", "Bengaluru"),
            new Venue("NCPA Tata Theatre", "Mumbai"),
            new Venue("St. Andrew's Auditorium", "Mumbai"),
            new Venue("Siri Fort Auditorium", "Delhi"),
            new Venue("Kamani Auditorium", "Delhi"),
            new Venue("Shilpakala Vedika", "Hyderabad"),
            new Venue("Rabindra Sadan", "Kolkata"),
            new Venue("Balgandharva Rangmandir", "Pune"));

    static final List<EventTemplate> EVENTS = List.of(
            new EventTemplate("Neon Skyline Live", EventCategory.CONCERT,
                    "An evening of synth-pop and indie anthems with a full light show."),
            new EventTemplate("Midnight Ragas", EventCategory.CONCERT,
                    "Hindustani classical meets ambient electronica in a late-night set."),
            new EventTemplate("Sufi Evenings", EventCategory.CONCERT,
                    "Qawwali and Sufi poetry performed by a twelve-piece ensemble."),
            new EventTemplate("Symphony Under the Stars", EventCategory.CONCERT,
                    "A chamber orchestra performs film scores from the last five decades."),
            new EventTemplate("Retro Bollywood Night", EventCategory.CONCERT,
                    "Live band covers of the biggest Hindi film songs from the 70s to the 90s."),
            new EventTemplate("Coastal Beats Festival", EventCategory.CONCERT,
                    "Three bands, one stage: surf rock, folk fusion and Konkani funk."),
            new EventTemplate("Jazz at the Terrace", EventCategory.CONCERT,
                    "A quartet playing standards and originals, with a short intermission."),
            new EventTemplate("Indie Nights: Echo Room", EventCategory.CONCERT,
                    "New independent artists from across India, one 25-minute set each."),
            new EventTemplate("Carnatic Strings", EventCategory.CONCERT,
                    "Veena and violin duets across ragas, with mridangam accompaniment."),
            new EventTemplate("Bass & Brass", EventCategory.CONCERT,
                    "A brass band reimagines electronic hits with live percussion."),
            new EventTemplate("Stand-Up Saturdays", EventCategory.COMEDY,
                    "Four comedians, forty minutes each. Recommended for ages 16 and up."),
            new EventTemplate("The Late Laugh Show", EventCategory.COMEDY,
                    "A late-night comedy lineup with surprise guests."),
            new EventTemplate("Improv Jam", EventCategory.COMEDY,
                    "Fully improvised scenes built from audience suggestions."),
            new EventTemplate("Office Hours: A Corporate Comedy", EventCategory.COMEDY,
                    "An hour of jokes about meetings, appraisals and reply-all emails."),
            new EventTemplate("Open Mic Finals", EventCategory.COMEDY,
                    "The season's best open-mic comics compete for the title."),
            new EventTemplate("Family Roast Night", EventCategory.COMEDY,
                    "Clean comedy about Indian families, suitable for all ages."),
            new EventTemplate("Hamlet Reimagined", EventCategory.THEATRE,
                    "Shakespeare's tragedy set in a modern Indian newsroom."),
            new EventTemplate("The Glass Menagerie", EventCategory.THEATRE,
                    "Tennessee Williams' memory play in a minimalist staging."),
            new EventTemplate("Tughlaq", EventCategory.THEATRE,
                    "Girish Karnad's classic about ambition and idealism."),
            new EventTemplate("Broadway Nights", EventCategory.THEATRE,
                    "A musical revue of Broadway favourites with a live orchestra."),
            new EventTemplate("Kathakali: The Tale of Nala", EventCategory.THEATRE,
                    "A traditional Kathakali performance with English surtitles."),
            new EventTemplate("The Mousetrap", EventCategory.THEATRE,
                    "Agatha Christie's long-running murder mystery."),
            new EventTemplate("Monsoon Monologues", EventCategory.THEATRE,
                    "Six short solo plays by emerging playwrights."),
            new EventTemplate("City Derby: Chennai vs Bengaluru", EventCategory.SPORTS,
                    "Football's fiercest southern rivalry, live at the stadium."),
            new EventTemplate("Kabaddi Premier Night", EventCategory.SPORTS,
                    "A double-header from the national kabaddi league."),
            new EventTemplate("Badminton Open Finals", EventCategory.SPORTS,
                    "Men's and women's singles finals of the city open."),
            new EventTemplate("3x3 Basketball Showdown", EventCategory.SPORTS,
                    "Sixteen teams, fast games, one knockout evening."),
            new EventTemplate("Table Tennis Masters", EventCategory.SPORTS,
                    "Top-ranked players compete in the national masters series."),
            new EventTemplate("DevSummit 2026", EventCategory.CONFERENCE,
                    "Talks on distributed systems, developer tooling and platform engineering."),
            new EventTemplate("Product Leaders Forum", EventCategory.CONFERENCE,
                    "Product heads from Indian startups on building for the next billion users."),
            new EventTemplate("Design Systems Day", EventCategory.CONFERENCE,
                    "A single-track conference about scaling design across teams."),
            new EventTemplate("Cloud Native Conf", EventCategory.CONFERENCE,
                    "Kubernetes, observability and cost control in production."),
            new EventTemplate("Fintech Futures", EventCategory.CONFERENCE,
                    "UPI, lending and payments infrastructure: what's next."),
            new EventTemplate("Data & AI Summit", EventCategory.CONFERENCE,
                    "Practical machine learning, data platforms and responsible AI."),
            new EventTemplate("Photography Masterclass", EventCategory.WORKSHOP,
                    "Composition, light and editing, with a live critique session."),
            new EventTemplate("Pottery for Beginners", EventCategory.WORKSHOP,
                    "Hands-on wheel throwing. All materials are provided."),
            new EventTemplate("Creative Writing Lab", EventCategory.WORKSHOP,
                    "Short fiction exercises and feedback from published authors."),
            new EventTemplate("Public Speaking Bootcamp", EventCategory.WORKSHOP,
                    "Structure, delivery and handling questions, with recorded practice."),
            new EventTemplate("Intro to Sound Design", EventCategory.WORKSHOP,
                    "Synthesis and sampling basics for film and games."));

    private static final Map<EventCategory, List<String>> IMAGES = Map.of(
            EventCategory.CONCERT, List.of(
                    "1501386761578-eac5c94b800a", "1470229722913-7c0e2dbbafd3", "1540039155733-5bb30b53aa14",
                    "1493225457124-a3eb161ffa5f", "1514525253161-7a46d19cd819", "1459749411175-04bf5292ceea",
                    "1492684223066-81342ee5ff30", "1524368535928-5b5e00ddc76b"),
            EventCategory.COMEDY, List.of(
                    "1527224857830-43a7acc85260", "1507676184212-d03ab07a01bf", "1475721027785-f74eccf877e2"),
            EventCategory.THEATRE, List.of(
                    "1503095396549-807759245b35", "1478147427282-58a87a120781", "1585699324551-f6c309eedeca"),
            EventCategory.SPORTS, List.of(
                    "1552667466-07770ae110d0", "1574629810360-7efbbe195018", "1546519638-68e109498ffc",
                    "1461896836934-ffe607ba8211"),
            EventCategory.CONFERENCE, List.of(
                    "1540575467063-178a50c2df87", "1505373877841-8d25f7d46678", "1531058020387-3be344556be6",
                    "1560439514-4e9645039924"),
            EventCategory.WORKSHOP, List.of(
                    "1591115765373-5207764f72e7", "1515169067868-5387ec356754", "1517457373958-b7bdd4587205"));

    static final List<String> CUSTOMER_NAMES = List.of(
            "Aarav Mehta", "Diya Iyer", "Kabir Singh", "Ananya Rao", "Rohan Gupta", "Ishita Nair",
            "Vikram Reddy", "Meera Pillai", "Arjun Das", "Sneha Kulkarni", "Karthik Subramanian",
            "Priya Menon", "Nikhil Joshi", "Aditi Banerjee", "Siddharth Varma", "Kavya Hegde",
            "Rahul Chatterjee", "Pooja Shetty", "Aman Kapoor", "Tara Krishnan");

    static String imageFor(EventCategory category, int index) {
        List<String> ids = IMAGES.get(category);
        return "https://images.unsplash.com/photo-" + ids.get(index % ids.size()) + "?w=1200&q=75&auto=format&fit=crop";
    }

    static SectionPricing pricingFor(EventCategory category) {
        return switch (category) {
            case CONCERT -> pricing(2499, 1499, 799);
            case COMEDY -> pricing(1299, 899, 499);
            case THEATRE -> pricing(1499, 999, 599);
            case SPORTS -> pricing(1999, 1199, 499);
            case CONFERENCE -> pricing(2999, 1999, 999);
            case WORKSHOP -> pricing(1499, 999, 699);
        };
    }

    static LocalTime startTimeFor(EventCategory category, Random random) {
        List<LocalTime> options = switch (category) {
            case CONFERENCE, WORKSHOP -> List.of(LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(14, 0));
            case SPORTS -> List.of(LocalTime.of(16, 0), LocalTime.of(19, 30));
            default -> List.of(LocalTime.of(18, 30), LocalTime.of(19, 0), LocalTime.of(19, 30), LocalTime.of(20, 0));
        };
        return options.get(random.nextInt(options.size()));
    }

    private static SectionPricing pricing(int vip, int premium, int standard) {
        return new SectionPricing(BigDecimal.valueOf(vip), BigDecimal.valueOf(premium), BigDecimal.valueOf(standard));
    }
}
