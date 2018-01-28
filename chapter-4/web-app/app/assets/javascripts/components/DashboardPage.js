'use strict';

import React from 'react';
import { Link } from 'react-router';
import NotFoundPage from './NotFoundPage';
import SearchResults from './SearchResults';
import { Loader, Input, Button} from 'semantic-ui-react';


export default class DashboardPage extends React.Component {
  constructor(){
    super();
    this.hostname = window.location.protocol + "//" + window.location.hostname + ":"+(window.location.port);
    this.state = {
      loggedIn : null,
      results: [],
      query: null,
      loading : false
    }
  }
  handleSearchClick(){
    this.setState({
      loading: true
    });
    return fetch(`${this.hostname}/api/v1/searchQuery?query=${this.state.query}`,{
      credentials:"same-origin"
    })
    .then(function(response) {
      if(response.status==200){
        return response.json()  
      } else {
        throw new Error(response.status)
      }
    }).then(body =>
      this.setState({
        loading: false,
        results : body.message
      })
    ).catch(err =>
      this.setState({
        loading: false,
        results:[]
      })
    )
  }

  componentDidMount(){
    return fetch(this.hostname+'/api/status',{
      credentials:"same-origin"
    })
    .then(function(response) {
      if(response.status==200){
        return response.json()  
      } else {
        throw new Error(response.status)
      }
    }).then(body =>
      this.setState({
        loggedIn: true
      })
    ).catch(err =>
      this.setState({
        loggedIn: false
      })
    )
  }
  render() {
    let content;
    if(this.state.loggedIn == null){
        content = <Loader active inline='centered' />  
    } else if(this.state.loggedIn == true) {
      // Dashboard view
        content = <div>
          <div>
            <Input fluid
            onChange={e => this.setState({query: e.target.value }) }
            size="massive"
            ref="query"
            placeholder='scala developers in singapore...'
            />
            <Button fluid secondary onClick={this.handleSearchClick.bind(this)}>Search</Button>
          </div>
          <SearchResults loading={this.state.loading} results={this.state.results} />
        </div>
    } else {
      // Redirect user to homepage
      window.location.href=this.hostname
    }
    return (
      <div className="athlete-full" style={{margin:"auto",marginRight:'30%',marginLeft:'30%'}}>
        {content}
      </div>
    );
  }
}
