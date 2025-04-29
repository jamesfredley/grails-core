package example.grails

import geb.Page

class HomePage extends Page {

    static url = '/'

    static at = { title.contains('Grails') }
}
