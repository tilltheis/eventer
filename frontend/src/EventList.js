import React from 'react';
import Table from 'react-bootstrap/Table'

export default class EventList extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      items: []
    };
  }

  componentDidMount() {
    fetch(process.env.REACT_APP_API_URL + '/events')
      .then(res => res.json())
      .then(
        (result) => {
          this.setState({
            isLoaded: true,
            items: result
          });
        },
        (error) => {
          this.setState({
            isLoaded: true,
            error
          });
        }
      );
  }

  render() {
    let Details = () => <></>;

    const { error, isLoaded, items } = this.state;
    if (error) {
      Details = () => <div>Error: {error.message}</div>;
    } else if (!isLoaded) {
      Details = () => <div>Loading...</div>;
    } else if (items.length === 0) {
      Details = () => <div>There are no events.</div>;
    } else {
      Details = () => (
        <Table responsive>
          <thead>
            <tr><th></th><th>Title</th><th>Host</th><th>Date</th><th>Guests</th><th>Description</th></tr>
          </thead>
          <tbody>
            {items.map(item => (
              <tr key={item.id}><td><a href={"/events/" + item.id}>✎</a></td><td>{item.title}</td><td>{item.host}</td><td>{item.dateTime}</td><td>Guests...</td><td>{item.description}</td></tr>
            ))}
          </tbody>
        </Table>
      );
    }

    return (
      <>
        <h2>My Events</h2>
        <Details />
      </>
    )
  }
};